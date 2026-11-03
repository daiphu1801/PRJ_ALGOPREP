package com.algoprep.bootstrap.problembank;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.algoprep.common.exception.ResourceNotFoundException;
import com.algoprep.judge.application.ports.out.ProblemGradingSpecPort;
import com.algoprep.problembank.application.ports.in.PublishTestcaseSnapshotUseCase;
import com.algoprep.problembank.application.ports.out.TestcaseObjectStorePort;
import com.algoprep.problembank.application.snapshot.TestcaseSnapshotCodec;
import com.algoprep.problembank.domain.exception.TestcaseStorageUnavailableException;
import com.algoprep.problembank.domain.model.SnapshotMetadata;
import com.algoprep.problembank.domain.ports.out.SnapshotPublicationRepository;
import io.minio.BucketExistsArgs;
import io.minio.ListObjectsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import java.io.ByteArrayOutputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import javax.tools.ToolProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.MountableFile;
import tools.jackson.databind.json.JsonMapper;

/** Publication kernel with real transactions and immutable object reads; no fake runtime ports. */
@SpringBootTest(
    properties =
        "spring.autoconfigure.exclude=org.springframework.boot.amqp.autoconfigure.RabbitAutoConfiguration")
@ActiveProfiles("test")
@Testcontainers
class SnapshotPublicationFlowTest {
  private static final String BUCKET = "algoprep-testcases";

  @Container @ServiceConnection
  static final PostgreSQLContainer POSTGRES =
      new PostgreSQLContainer("postgres:16.15-alpine")
          .withDatabaseName("algoprep")
          .withCopyFileToContainer(
              MountableFile.forHostPath("../../database/init/01-schemas.sql"),
              "/docker-entrypoint-initdb.d/01-schemas.sql");

  @Container
  static final GenericContainer<?> REDIS =
      new GenericContainer<>("redis:7.4-alpine").withExposedPorts(6379);

  @Container
  static final GenericContainer<?> MINIO =
      new GenericContainer<>("minio/minio:RELEASE.2025-09-07T16-13-09Z")
          .withEnv("MINIO_ROOT_USER", "test-snapshot-user")
          .withEnv("MINIO_ROOT_PASSWORD", "test-snapshot-password")
          .withCommand("server", "/data")
          .withExposedPorts(9000)
          .waitingFor(Wait.forHttp("/minio/health/ready").forPort(9000));

  @DynamicPropertySource
  static void infrastructure(DynamicPropertyRegistry properties) {
    properties.add("spring.data.redis.host", REDIS::getHost);
    properties.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
    properties.add(
        "algoprep.problem-bank.storage.endpoint",
        () -> "http://" + MINIO.getHost() + ":" + MINIO.getMappedPort(9000));
    properties.add("algoprep.problem-bank.storage.access-key", () -> "test-snapshot-user");
    properties.add("algoprep.problem-bank.storage.secret-key", () -> "test-snapshot-password");
  }

  @Autowired JdbcTemplate jdbc;
  @Autowired PublishTestcaseSnapshotUseCase writer;
  @Autowired ProblemGradingSpecPort reader;
  @Autowired SnapshotPublicationRepository repository;
  @Autowired TestcaseObjectStorePort storage;
  @Autowired TestcaseSnapshotCodec codec;
  @Autowired TransactionTemplate transaction;
  @Autowired ObjectProvider<JdbcClient> jdbcProvider;

  @Autowired
  @Qualifier("testcaseServiceClient")
  MinioClient minio;

  @TempDir Path temporary;
  private UUID id;
  private UUID draft;
  private List<UUID> approved;
  private byte[] large;

  @BeforeEach
  void fixture() throws Exception {
    if (!minio.bucketExists(BucketExistsArgs.builder().bucket(BUCKET).build())) {
      minio.makeBucket(MakeBucketArgs.builder().bucket(BUCKET).build());
    }
    id = UUID.randomUUID();
    draft = UUID.randomUUID();
    large = new byte[8193];
    new java.util.Random(42).nextBytes(large);
    seedProblem();
    approved = new ArrayList<>();
    for (int i = 1; i <= 10; i++) {
      UUID testcaseId = UUID.randomUUID();
      approved.add(testcaseId);
      addCase(testcaseId, i, i <= 2 ? "SAMPLE" : "HIDDEN", true);
    }
    addCase(draft, 11, "SAMPLE", false);
    addCase(UUID.randomUUID(), 12, "HIDDEN", false);
  }

  private void seedProblem() {
    UUID actor = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO identity.users(id,email,display_name,role_id,status)"
            + " SELECT ?,?,'Snapshot actor',id,'ACTIVE' FROM identity.roles WHERE code='INSTRUCTOR'",
        actor,
        actor + "@example.test");
    jdbc.update(
        "INSERT INTO problem.problems(id,code,title,author_id,updated_by,level_id,time_limit_ms,"
            + "memory_limit_mb,max_output_size_kb) VALUES (?,?,'Snapshot fixture',?,?,'20000000-0000-4000-8000-000000000001',1000,256,64)",
        id,
        id.toString(),
        actor,
        actor);
    jdbc.update(
        "INSERT INTO problem.problem_specs(id,problem_id,matching_strategy,spec_version) VALUES (?,?,'EXACT',1)",
        UUID.randomUUID(),
        id);
  }

  private void addCase(UUID testcaseId, int position, String visibility, boolean approvedCase) {
    boolean inputObject = position == 2;
    boolean outputObject = position == 1;
    String inputKey = sourceKey(testcaseId, "input");
    String outputKey = sourceKey(testcaseId, "expected_output");
    if (inputObject) {
      storage.put(inputKey, large);
    }
    if (outputObject) {
      storage.put(outputKey, large);
    }
    jdbc.update(
        "INSERT INTO problem.testcases(id,problem_id,visibility,position,approved,input_storage,"
            + "input_inline,input_object_key,expected_output_storage,expected_output_inline,expected_output_object_key)"
            + " VALUES (?,?,?,?,?,?,?,?,?,?,?)",
        testcaseId,
        id,
        visibility,
        position,
        approvedCase,
        inputObject ? "MINIO" : "INLINE",
        inputObject ? null : position == 1 ? "" : position + "\n",
        inputObject ? inputKey : null,
        outputObject ? "MINIO" : "INLINE",
        outputObject ? null : position == 2 ? "đáp án\r\n" : (position + 1) + "\n",
        outputObject ? outputKey : null);
  }

  private String sourceKey(UUID testcaseId, String side) {
    return "problems/" + id + "/testcases/" + testcaseId + "/" + side + ".txt";
  }

  private SnapshotMetadata metadata(int version) {
    return jdbc.queryForObject(
        "SELECT * FROM problem.testcase_set_snapshots WHERE problem_id=? AND version=?",
        (rs, row) ->
            new SnapshotMetadata(
                id,
                version,
                rs.getString("object_key"),
                rs.getInt("testcase_count"),
                rs.getString("content_sha256")),
        id,
        version);
  }

  private int currentVersion() {
    return jdbc.queryForObject(
        "SELECT current_testcase_set_version FROM problem.problems WHERE id=?", Integer.class, id);
  }

  private long events() {
    return jdbc.queryForObject(
        "SELECT count(*) FROM problem.outbox_events WHERE aggregate_id=?", Long.class, id);
  }

  private List<String> objectKeys() throws Exception {
    List<String> result = new ArrayList<>();
    for (var item :
        minio.listObjects(
            ListObjectsArgs.builder()
                .bucket(BUCKET)
                .prefix("problems/" + id + "/snapshots/")
                .recursive(true)
                .build())) {
      result.add(item.get().objectName());
    }
    return result;
  }

  @Test
  void firstPublicationCreatesTheCurrentVersionFromOnlyApprovedRealBytes() throws Exception {
    var result = writer.publishSnapshot(id);
    assertThat(result.version()).isEqualTo(1);
    assertThat(result.created()).isTrue();
    assertThat(result.versionBumped()).isFalse();
    assertThat(result.testcaseCount()).isEqualTo(10);
    assertThat(currentVersion()).isEqualTo(1);
    assertThat(events()).isZero();
    var metadata = metadata(1);
    byte[] bytes = storage.get(metadata.objectKey());
    assertThat(codec.sha256(bytes)).isEqualTo(metadata.contentSha256());
    var snapshot = reader.loadTestcaseSnapshot(id, 1);
    assertThat(snapshot.testcases())
        .extracting(row -> row.testcaseId())
        .containsExactlyElementsOf(approved);
    assertThat(snapshot.testcases().getFirst().input()).isEmpty();
    assertThat(snapshot.testcases().getFirst().expectedOutput()).containsExactly(large);
    assertThat(snapshot.testcases().get(1).input()).containsExactly(large);
    assertThat(snapshot.testcases().get(1).expectedOutput())
        .containsExactly("đáp án\r\n".getBytes(StandardCharsets.UTF_8));
    assertThat(reader.loadGradingSpec(id).orElseThrow().problem().snapshotReady()).isTrue();
    assertThat(reader.loadGradingSpec(id).orElseThrow().problem().published()).isFalse();
    assertThat(objectKeys()).containsExactly(metadata.objectKey());
  }

  @Test
  void noChangeReusesMetadataObjectAndDatabaseTimestamps() throws Exception {
    writer.publishSnapshot(id);
    String before = databaseState();
    var metadata = metadata(1);
    var result = writer.publishSnapshot(id);
    assertThat(result.created()).isFalse();
    assertThat(result.versionBumped()).isFalse();
    assertThat(result.version()).isEqualTo(1);
    assertThat(databaseState()).isEqualTo(before);
    assertThat(objectKeys()).containsExactly(metadata.objectKey());
  }

  @Test
  void changedOutputCommitsOneVersionAndOutboxWhilePreservingTheOldObject() {
    writer.publishSnapshot(id);
    var old = metadata(1);
    byte[] oldBytes = storage.get(old.objectKey());
    editAndPublish(
        () -> {
          jdbc.update(
              "UPDATE problem.testcases SET expected_output_inline='changed' WHERE id=?",
              approved.get(2));
        });
    assertThat(currentVersion()).isEqualTo(2);
    assertThat(events()).isEqualTo(1);
    assertThat(storage.get(old.objectKey())).containsExactly(oldBytes);
    assertThat(reader.loadTestcaseSnapshot(id, 1).testcases().get(2).expectedOutput())
        .containsExactly("4\n".getBytes(StandardCharsets.UTF_8));
    assertThat(reader.loadTestcaseSnapshot(id, 2).testcases().get(2).expectedOutput())
        .containsExactly("changed".getBytes(StandardCharsets.UTF_8));
    assertBumpEvent();
  }

  private void assertBumpEvent() {
    var event =
        jdbc.queryForMap(
            "SELECT id,routing_key,payload::text AS envelope FROM problem.outbox_events WHERE aggregate_id=?",
            id);
    var json = JsonMapper.builder().build().readTree((String) event.get("envelope"));
    assertThat(json.get("eventId").asText()).isEqualTo(event.get("id").toString());
    assertThat(json.get("eventType").asText()).isEqualTo("TestcaseSetVersionBumped");
    assertThat(json.get("schemaVersion").asInt()).isEqualTo(1);
    assertThat(event.get("routing_key")).isEqualTo("problem.testcase-set.version-bumped");
    assertThat(json.get("payload").get("problemId").asText()).isEqualTo(id.toString());
    assertThat(json.get("payload").get("testcaseSetVersion").asInt()).isEqualTo(2);
    assertThat(json.get("payload").get("bumpedAt").asText())
        .isEqualTo(json.get("occurredAt").asText());
  }

  @Test
  void draftAndCategoryEditsDoNotChangeTheGradingSet() {
    writer.publishSnapshot(id);
    jdbc.update("UPDATE problem.testcases SET input_inline='draft edit' WHERE id=?", draft);
    jdbc.update("UPDATE problem.testcases SET category='BOUNDARY_LOW' WHERE id=?", approved.get(2));
    assertThat(writer.publishSnapshot(id).created()).isFalse();
    assertThat(currentVersion()).isEqualTo(1);
    assertThat(events()).isZero();
  }

  @Test
  void storageRepresentationChangesWithIdenticalBytesDoNotBumpTheVersion() {
    writer.publishSnapshot(id);
    String key = sourceKey(approved.get(2), "migrated_input");
    storage.put(key, "3\n".getBytes(StandardCharsets.UTF_8));
    jdbc.update(
        "UPDATE problem.testcases SET input_storage='MINIO',input_inline=NULL,input_object_key=? WHERE id=?",
        key,
        approved.get(2));
    assertThat(writer.publishSnapshot(id).created()).isFalse();
    assertThat(currentVersion()).isEqualTo(1);
  }

  @Test
  void reorderedApprovedCasesCreateOneNewVersionWithTheNewOrder() {
    writer.publishSnapshot(id);
    editAndPublish(
        () -> {
          jdbc.update(
              "UPDATE problem.testcases SET position=CASE id WHEN ? THEN 2 WHEN ? THEN 1 ELSE position END WHERE problem_id=?",
              approved.getFirst(),
              approved.get(1),
              id);
        });
    assertThat(events()).isEqualTo(1);
    assertThat(reader.loadTestcaseSnapshot(id, 1).testcases().getFirst().testcaseId())
        .isEqualTo(approved.getFirst());
    assertThat(reader.loadTestcaseSnapshot(id, 2).testcases().getFirst().testcaseId())
        .isEqualTo(approved.get(1));
  }

  @Test
  void snapshotOwnsBytesEvenAfterMutableSourceObjectsChange() {
    writer.publishSnapshot(id);
    storage.put(sourceKey(approved.get(1), "input"), new byte[] {99});
    assertThat(reader.loadTestcaseSnapshot(id, 1).testcases().get(1).input())
        .containsExactly(large);
  }

  @ParameterizedTest
  @ValueSource(strings = {"SNAPSHOT", "OUTBOX"})
  void databaseFailuresRollBackTheCallerEditMetadataVersionAndOutbox(String phase)
      throws Exception {
    writer.publishSnapshot(id);
    String before = databaseState();
    String table = phase.equals("SNAPSHOT") ? "testcase_set_snapshots" : "outbox_events";
    jdbc.execute(
        "CREATE FUNCTION problem.reject_snapshot_fixture() RETURNS trigger LANGUAGE plpgsql AS $$"
            + " BEGIN RAISE EXCEPTION 'Snapshot fixture unavailable'; END; $$");
    jdbc.execute(
        "CREATE TRIGGER reject_snapshot_fixture BEFORE INSERT ON problem."
            + table
            + " FOR EACH ROW EXECUTE FUNCTION problem.reject_snapshot_fixture()");
    try {
      unavailable(
          () ->
              editAndPublish(
                  () -> {
                    jdbc.update(
                        "UPDATE problem.testcases SET expected_output_inline='must rollback' WHERE id=?",
                        approved.get(2));
                  }));
    } finally {
      jdbc.execute("DROP TRIGGER reject_snapshot_fixture ON problem." + table);
      jdbc.execute("DROP FUNCTION problem.reject_snapshot_fixture()");
    }
    assertThat(databaseState()).isEqualTo(before);
    assertThat(objectKeys()).hasSize(2);
    assertThat(reader.loadTestcaseSnapshot(id, 1).testcases().get(2).expectedOutput())
        .containsExactly("4\n".getBytes(StandardCharsets.UTF_8));
  }

  @Test
  void callerRollbackLeavesOnlyAnUnreferencedObjectAndRetryUsesAFreshKey() throws Exception {
    transaction.executeWithoutResult(
        status -> {
          writer.publishSnapshot(id);
          status.setRollbackOnly();
        });
    String orphan = objectKeys().getFirst();
    byte[] orphanBytes = storage.get(orphan);
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM problem.testcase_set_snapshots WHERE problem_id=?",
                Integer.class,
                id))
        .isZero();
    writer.publishSnapshot(id);
    assertThat(metadata(1).objectKey()).isNotEqualTo(orphan);
    assertThat(storage.get(orphan)).containsExactly(orphanBytes);
    assertThat(objectKeys()).hasSize(2);
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void concurrentPublicationsProduceOneSnapshotForTheSameSet(boolean existing) throws Exception {
    if (existing) {
      writer.publishSnapshot(id);
      jdbc.update(
          "UPDATE problem.testcases SET expected_output_inline='draft revision' WHERE id=?",
          approved.get(2));
    }
    var gate = new CountDownLatch(1);
    try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
      var first =
          executor.submit(
              () -> {
                gate.await();
                return writer.publishSnapshot(id);
              });
      var second =
          executor.submit(
              () -> {
                gate.await();
                return writer.publishSnapshot(id);
              });
      gate.countDown();
      var results = List.of(first.get(20, TimeUnit.SECONDS), second.get(20, TimeUnit.SECONDS));
      assertThat(results).allSatisfy(row -> assertThat(row.version()).isEqualTo(existing ? 2 : 1));
      assertThat(results.stream().filter(row -> row.created()).count()).isEqualTo(1);
    }
    assertThat(currentVersion()).isEqualTo(existing ? 2 : 1);
    assertThat(objectKeys()).hasSize(existing ? 2 : 1);
    assertThat(events()).isEqualTo(existing ? 1 : 0);
  }

  @ParameterizedTest
  @ValueSource(strings = {"MISSING", "CORRUPT"})
  void brokenPublishedSnapshotIsNotSilentlyReplaced(String fault) {
    writer.publishSnapshot(id);
    var old = metadata(1);
    if (fault.equals("MISSING")) {
      storage.delete(old.objectKey());
    } else {
      jdbc.update(
          "UPDATE problem.testcase_set_snapshots SET content_sha256=? WHERE problem_id=?",
          "0".repeat(64),
          id);
    }
    String before = databaseState();
    unavailable(() -> writer.publishSnapshot(id));
    assertThat(databaseState()).isEqualTo(before);
    assertThat(currentVersion()).isEqualTo(1);
  }

  @Test
  void missingCurrentMinioInputRollsBackTheCallerEditWithoutChangingOldSnapshots() {
    writer.publishSnapshot(id);
    storage.delete(sourceKey(approved.get(1), "input"));
    String before = databaseState();
    unavailable(
        () ->
            editAndPublish(
                () -> {
                  jdbc.update(
                      "UPDATE problem.testcases SET expected_output_inline='must rollback' WHERE id=?",
                      approved.get(2));
                }));
    assertThat(databaseState()).isEqualTo(before);
    assertThat(reader.loadTestcaseSnapshot(id, 1).testcases().get(1).input())
        .containsExactly(large);
  }

  @Test
  void emptyApprovedSetCannotBePublished() throws Exception {
    jdbc.update("UPDATE problem.testcases SET approved=false WHERE problem_id=?", id);
    unavailable(() -> writer.publishSnapshot(id));
    assertThat(objectKeys()).isEmpty();
    assertThat(currentVersion()).isEqualTo(1);
  }

  @Test
  void missingAndDeletedProblemsCannotBePublishedButPinnedHistoryRemainsReadable() {
    writer.publishSnapshot(id);
    jdbc.update("UPDATE problem.problems SET deleted=true WHERE id=?", id);
    for (UUID target : List.of(id, UUID.randomUUID())) {
      assertThatThrownBy(() -> writer.publishSnapshot(target))
          .isInstanceOfSatisfying(
              ResourceNotFoundException.class,
              failure -> assertThat(failure.errorCode()).isEqualTo("PRB-010"));
    }
    assertThat(reader.loadTestcaseSnapshot(id, 1).testcases()).hasSize(10);
  }

  @Test
  void logicalComparisonIgnoresSerializationWhitespaceOfAnImportedSnapshot() {
    writer.publishSnapshot(id);
    var old = metadata(1);
    var json = JsonMapper.builder().build();
    byte[] pretty =
        json.writerWithDefaultPrettyPrinter()
            .writeValueAsBytes(json.readTree(storage.get(old.objectKey())));
    String importedKey = "problems/" + id + "/legacy/1.pretty.json";
    storage.put(importedKey, pretty);
    jdbc.update(
        "UPDATE problem.testcase_set_snapshots SET object_key=?,content_sha256=? WHERE problem_id=?",
        importedKey,
        codec.sha256(pretty),
        id);
    assertThat(writer.publishSnapshot(id).created()).isFalse();
    assertThat(currentVersion()).isEqualTo(1);
    assertThat(metadata(1).objectKey()).isEqualTo(importedKey);
    assertThat(events()).isZero();
  }

  private String databaseState() {
    return jdbc.queryForObject(
        "SELECT jsonb_build_object('problem',(SELECT to_jsonb(p) FROM problem.problems p WHERE id=?),"
            + "'cases',(SELECT jsonb_agg(t ORDER BY t.id) FROM problem.testcases t WHERE problem_id=?),"
            + "'snapshots',(SELECT jsonb_agg(b ORDER BY version) FROM problem.testcase_set_snapshots b WHERE problem_id=?),"
            + "'outbox',(SELECT jsonb_agg(o ORDER BY o.id) FROM problem.outbox_events o WHERE aggregate_id=?))::text",
        String.class,
        id,
        id,
        id,
        id);
  }

  private void editAndPublish(Runnable edit) {
    transaction.executeWithoutResult(
        status -> {
          repository.lockProblem(id).orElseThrow();
          edit.run();
          writer.publishSnapshot(id);
        });
  }

  @ParameterizedTest
  @ValueSource(strings = {"APPROVED", "DELETED"})
  void copiedSqlCannotPublishDraftsOrDeletedProblems(String rule) throws Exception {
    if (rule.equals("DELETED")) {
      jdbc.update("UPDATE problem.problems SET deleted=true WHERE id=?", id);
    }
    Path file =
        Path.of(
            "../algoprep-problem-bank/src/main/java/com/algoprep/problembank/infrastructure/persistence/JdbcSnapshotPublicationRepository.java");
    String source = Files.readString(file);
    String mutant =
        rule.equals("APPROVED")
            ? source.replace("t.approved=true", "true")
            : source.replace("AND NOT p.deleted", "");
    assertThat(mutant).isNotEqualTo(source);
    sqlCandidate(source, rule, false);
    sqlCandidate(mutant, rule, true);
    assertThat(Files.readString(file)).isEqualTo(source);
    System.out.println(
        "MUTATION CAUGHT SNAPSHOT_WRITE_SQL_" + rule + "; original GREEN; mutant RED");
  }

  private void sqlCandidate(String source, String rule, boolean mutant) throws Exception {
    String prefix = "com.algoprep.problembank.infrastructure.persistence";
    Path folder = Files.createDirectories(temporary.resolve(mutant ? "mutant" : "original"));
    Path file = folder.resolve("JdbcSnapshotPublicationRepository.java");
    Files.writeString(
        file, source.replace("package " + prefix + ";", "package " + prefix + ".mutationcopy;"));
    var errors = new ByteArrayOutputStream();
    int code =
        ToolProvider.getSystemJavaCompiler()
            .run(
                null,
                errors,
                errors,
                "-proc:none",
                "-encoding",
                "UTF-8",
                "-classpath",
                System.getProperty("java.class.path"),
                "-d",
                folder.toString(),
                file.toString());
    assertThat(code).withFailMessage(errors.toString(StandardCharsets.UTF_8)).isZero();
    try (var loader =
        new URLClassLoader(new URL[] {folder.toUri().toURL()}, getClass().getClassLoader())) {
      var type = loader.loadClass(prefix + ".mutationcopy.JdbcSnapshotPublicationRepository");
      var constructor = type.getDeclaredConstructor(ObjectProvider.class);
      constructor.setAccessible(true);
      var candidate = (SnapshotPublicationRepository) constructor.newInstance(jdbcProvider);
      Runnable oracle =
          () -> {
            if (rule.equals("APPROVED")) {
              assertThat(candidate.findApproved(id))
                  .extracting(row -> row.descriptor().testcaseId())
                  .containsExactlyElementsOf(approved);
            } else {
              assertThat(candidate.lockProblem(id)).isEmpty();
            }
          };
      if (mutant) {
        assertThatThrownBy(oracle::run).isInstanceOf(AssertionError.class);
      } else {
        oracle.run();
      }
    }
  }

  private static void unavailable(Runnable operation) {
    assertThatThrownBy(operation::run)
        .isInstanceOfSatisfying(
            TestcaseStorageUnavailableException.class,
            failure -> {
              assertThat(failure.errorCode()).isEqualTo("PRB-052");
              assertThat(failure.getCause()).isNull();
              assertThat(failure.getSuppressed()).isEmpty();
            });
  }
}
