package com.algoprep.bootstrap.judge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.algoprep.judge.application.ports.out.ProblemGradingSpecPort;
import com.algoprep.judge.domain.exception.ProblemGradingReadException;
import com.algoprep.problembank.application.ports.out.TestcaseObjectStorePort;
import com.algoprep.problembank.application.snapshot.TestcaseSnapshotCodec;
import com.algoprep.problembank.domain.model.TestcaseData;
import com.algoprep.problembank.domain.ports.out.GradingReadRepository;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import java.io.ByteArrayOutputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
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

/** Real PG/Flyway/MinIO, wired through F2 -> bootstrap -> F4. No runtime reader stand-ins. */
@SpringBootTest(
    properties =
        "spring.autoconfigure.exclude=org.springframework.boot.amqp.autoconfigure.RabbitAutoConfiguration")
@ActiveProfiles("test")
@Testcontainers
class ProblemGradingReadFlowTest {
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
          .withEnv("MINIO_ROOT_USER", "test-grading-user")
          .withEnv("MINIO_ROOT_PASSWORD", "test-grading-password")
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
    properties.add("algoprep.problem-bank.storage.access-key", () -> "test-grading-user");
    properties.add("algoprep.problem-bank.storage.secret-key", () -> "test-grading-password");
  }

  @Autowired JdbcTemplate jdbc;
  @Autowired ProblemGradingSpecPort reader;
  @Autowired GradingReadRepository repository;
  @Autowired TestcaseObjectStorePort storage;
  @Autowired TestcaseSnapshotCodec codec;
  @Autowired TransactionTemplate transaction;
  @Autowired ObjectProvider<JdbcClient> jdbcProvider;

  @Autowired
  @Qualifier("testcaseServiceClient")
  MinioClient minio;

  @TempDir Path temporary;
  private UUID id;
  private UUID sampleInline;
  private UUID sampleMinio;
  private UUID hidden;
  private byte[] large;

  @BeforeEach
  void fixture() throws Exception {
    if (!minio.bucketExists(BucketExistsArgs.builder().bucket("algoprep-testcases").build())) {
      minio.makeBucket(MakeBucketArgs.builder().bucket("algoprep-testcases").build());
    }
    id = UUID.randomUUID();
    sampleInline = UUID.randomUUID();
    sampleMinio = UUID.randomUUID();
    hidden = UUID.randomUUID();
    large = new byte[8193];
    new java.util.Random(42).nextBytes(large);
    seedProblem();
    String inputKey = "problems/" + id + "/testcases/" + sampleMinio + "/input.txt";
    String outputKey = "problems/" + id + "/testcases/" + sampleInline + "/expected_output.txt";
    storage.put(inputKey, large);
    storage.put(outputKey, large);
    jdbc.update(
        "INSERT INTO problem.testcases(id,problem_id,visibility,position,approved,input_storage,"
            + "input_inline,expected_output_storage,expected_output_object_key) VALUES (?,?,'SAMPLE',1,true,'INLINE','','MINIO',?)",
        sampleInline,
        id,
        outputKey);
    jdbc.update(
        "INSERT INTO problem.testcases(id,problem_id,visibility,position,approved,input_storage,"
            + "input_object_key,expected_output_storage,expected_output_inline) VALUES (?,?,'SAMPLE',2,true,'MINIO',?,'INLINE','tiếng Việt\r\n')",
        sampleMinio,
        id,
        inputKey);
    inlineCase(hidden, "HIDDEN", 3, true);
    inlineCase(UUID.randomUUID(), "SAMPLE", 4, false);
    inlineCase(UUID.randomUUID(), "HIDDEN", 5, false);
    publishFixture(7, originalCases());
  }

  private void seedProblem() {
    UUID actor = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO identity.users(id,email,display_name,role_id,status)"
            + " SELECT ?,?,'Grading actor',id,'ACTIVE' FROM identity.roles WHERE code='INSTRUCTOR'",
        actor,
        actor + "@example.test");
    jdbc.update(
        "INSERT INTO problem.problems(id,code,title,author_id,updated_by,level_id,time_limit_ms,"
            + "memory_limit_mb,max_output_size_kb,status,current_testcase_set_version)"
            + " VALUES (?,?,'Grading fixture',?,?,'20000000-0000-4000-8000-000000000001',1700,384,96,'PUBLISHED',7)",
        id,
        id.toString(),
        actor,
        actor);
    jdbc.update(
        "INSERT INTO problem.problem_specs(id,problem_id,matching_strategy,epsilon_value,spec_version,"
            + "function_name,return_type,parameters) VALUES (?,?,'EPSILON',0.000000000001,130,'addOne',"
            + "'{\"kind\":\"INT\"}'::jsonb,'[{\"name\":\"value\",\"type\":{\"kind\":\"INT\"}}]'::jsonb)",
        UUID.randomUUID(),
        id);
  }

  private void inlineCase(UUID testcaseId, String visibility, int position, boolean approved) {
    jdbc.update(
        "INSERT INTO problem.testcases(id,problem_id,visibility,position,approved,input_storage,"
            + "input_inline,expected_output_storage,expected_output_inline) VALUES (?,?,?,?,?,'INLINE','secret-input','INLINE','secret-output')",
        testcaseId,
        id,
        visibility,
        position,
        approved);
  }

  private List<TestcaseData> originalCases() {
    return List.of(
        new TestcaseData(sampleInline, "SAMPLE", 1, new byte[0], large),
        new TestcaseData(
            sampleMinio, "SAMPLE", 2, large, "tiếng Việt\r\n".getBytes(StandardCharsets.UTF_8)),
        new TestcaseData(
            hidden,
            "HIDDEN",
            3,
            "secret-input".getBytes(StandardCharsets.UTF_8),
            "secret-output".getBytes(StandardCharsets.UTF_8)));
  }

  private String key(int version) {
    return "problems/" + id + "/snapshots/" + version + ".json";
  }

  private void publishFixture(int version, List<TestcaseData> cases) {
    byte[] bytes = codec.encode(id, version, cases);
    storage.put(key(version), bytes);
    jdbc.update(
        "INSERT INTO problem.testcase_set_snapshots(problem_id,version,object_key,testcase_count,content_sha256)"
            + " VALUES (?,?,?,?,?)",
        id,
        version,
        key(version),
        cases.size(),
        codec.sha256(bytes));
  }

  @Test
  void gradingMetadataHasOnlyApprovedCasesAndCoherentRawConfiguration() {
    var result = reader.loadGradingSpec(id).orElseThrow();
    assertThat(result.testcases())
        .extracting(row -> row.testcaseId())
        .containsExactly(sampleInline, sampleMinio, hidden);
    assertThat(result.problem().published()).isTrue();
    assertThat(result.problem().snapshotReady()).isTrue();
    assertThat(result.versions().specVersion()).isEqualTo(130);
    assertThat(result.versions().currentTestcaseSetVersion()).isEqualTo(7);
    assertThat(result.limits().timeLimitMs()).isEqualTo(1700);
    assertThat(result.limits().memoryLimitMb()).isEqualTo(384);
    assertThat(result.limits().maxOutputSizeKb()).isEqualTo(96);
    assertThat(result.limits().maxSubmissionsPerHour()).isNull();
    assertThat(result.matching().epsilonValue()).isEqualByComparingTo("0.000000000001");
  }

  @Test
  void samplesReadBothStorageBranchesWithoutHiddenOrUnapprovedContent() {
    var rows = reader.loadSampleTestcases(id);
    assertThat(rows).extracting(row -> row.testcaseId()).containsExactly(sampleInline, sampleMinio);
    assertThat(rows).allSatisfy(row -> assertThat(row.visibility()).isEqualTo("SAMPLE"));
    assertThat(rows.getFirst().input()).isEmpty();
    assertThat(rows.getFirst().expectedOutput()).containsExactly(large);
    assertThat(rows.getLast().input()).containsExactly(large);
    assertThat(rows.getLast().expectedOutput())
        .containsExactly("tiếng Việt\r\n".getBytes(StandardCharsets.UTF_8));
  }

  @Test
  void pinnedSnapshotSurvivesCurrentCaseDeletionAndWithdrawalOrSoftDeletion() {
    jdbc.update("DELETE FROM problem.testcases WHERE problem_id=?", id);
    jdbc.update(
        "UPDATE problem.problems SET status='UNPUBLISHED',deleted=true,current_testcase_set_version=8 WHERE id=?",
        id);
    assertThat(reader.loadGradingSpec(id).orElseThrow().problem().published()).isFalse();
    var pinned = reader.loadTestcaseSnapshot(id, 7);
    assertThat(pinned.version()).isEqualTo(7);
    assertThat(pinned.testcases())
        .extracting(row -> row.testcaseId())
        .containsExactly(sampleInline, sampleMinio, hidden);
    assertThat(pinned.testcases().getFirst().expectedOutput()).containsExactly(large);
    assertThat(reader.loadSampleTestcases(id)).isEmpty();
  }

  @Test
  void oldAndNewSnapshotsStayIndependentAfterTheCurrentSetChanges() {
    var changed = List.of(new TestcaseData(hidden, "HIDDEN", 1, new byte[] {8}, new byte[] {9}));
    publishFixture(8, changed);
    jdbc.update("UPDATE problem.problems SET current_testcase_set_version=8 WHERE id=?", id);
    assertThat(reader.loadTestcaseSnapshot(id, 7).testcases()).hasSize(3);
    assertThat(reader.loadTestcaseSnapshot(id, 8).testcases()).hasSize(1);
    assertThat(reader.loadTestcaseSnapshot(id, 8).testcases().getFirst().input())
        .containsExactly(8);
  }

  @Test
  void missingMetadataAndMissingObjectNeverFallBackToCurrentCases() {
    reason(
        () -> reader.loadTestcaseSnapshot(id, 6),
        ProblemGradingReadException.Reason.SPEC_UNAVAILABLE);
    storage.delete(key(7));
    assertThat(reader.loadGradingSpec(id).orElseThrow().problem().snapshotReady()).isFalse();
    reason(
        () -> reader.loadTestcaseSnapshot(id, 7),
        ProblemGradingReadException.Reason.SPEC_UNAVAILABLE);
    assertThat(reader.loadSampleTestcases(id)).hasSize(2);
  }

  @Test
  void snapshotWithoutMetadataIsNotReadyEvenIfAnObjectExists() {
    jdbc.update("DELETE FROM problem.testcase_set_snapshots WHERE problem_id=?", id);
    assertThat(storage.head(key(7))).isPositive();
    assertThat(reader.loadGradingSpec(id).orElseThrow().problem().snapshotReady()).isFalse();
  }

  @Test
  void wrongChecksumIsADataFailureWithoutHiddenContentOrObjectKeys() {
    jdbc.update(
        "UPDATE problem.testcase_set_snapshots SET content_sha256=? WHERE problem_id=?",
        "0".repeat(64),
        id);
    reason(
        () -> reader.loadTestcaseSnapshot(id, 7),
        ProblemGradingReadException.Reason.SPEC_UNAVAILABLE);
  }

  @Test
  void wrongBodyVersionWithAValidChecksumIsStillRejected() {
    byte[] wrong = codec.encode(id, 8, originalCases());
    storage.put(key(7), wrong);
    jdbc.update(
        "UPDATE problem.testcase_set_snapshots SET content_sha256=? WHERE problem_id=?",
        codec.sha256(wrong),
        id);
    reason(
        () -> reader.loadTestcaseSnapshot(id, 7),
        ProblemGradingReadException.Reason.SPEC_UNAVAILABLE);
  }

  @Test
  void sqlFailureIsInfrastructureFailureAndRecoversAfterRepair() {
    jdbc.execute("ALTER TABLE problem.testcase_set_snapshots RENAME TO unavailable_snapshots");
    try {
      reason(
          () -> reader.loadTestcaseSnapshot(id, 7),
          ProblemGradingReadException.Reason.SANDBOX_FAULT);
      reason(() -> reader.loadGradingSpec(id), ProblemGradingReadException.Reason.SANDBOX_FAULT);
    } finally {
      jdbc.execute("ALTER TABLE problem.unavailable_snapshots RENAME TO testcase_set_snapshots");
    }
    assertThat(reader.loadTestcaseSnapshot(id, 7).testcases()).hasSize(3);
  }

  @Test
  void unknownProblemIsAbsentAndAbsentSpecIsADataFailure() {
    assertThat(reader.loadGradingSpec(UUID.randomUUID())).isEmpty();
    jdbc.update("DELETE FROM problem.problem_specs WHERE problem_id=?", id);
    reason(() -> reader.loadGradingSpec(id), ProblemGradingReadException.Reason.SPEC_UNAVAILABLE);
  }

  @Test
  void readsDoNotWriteVersionsSnapshotsOutboxOrCurrentCases() {
    String before = databaseState();
    reader.loadGradingSpec(id);
    reader.loadSampleTestcases(id);
    reader.loadTestcaseSnapshot(id, 7);
    assertThat(databaseState()).isEqualTo(before);
  }

  private String databaseState() {
    return jdbc.queryForObject(
        "SELECT jsonb_build_object('problem',(SELECT to_jsonb(p) FROM problem.problems p WHERE id=?),"
            + "'cases',(SELECT jsonb_agg(t ORDER BY t.id) FROM problem.testcases t WHERE problem_id=?),"
            + "'snapshots',(SELECT jsonb_agg(b ORDER BY version) FROM problem.testcase_set_snapshots b WHERE problem_id=?),"
            + "'outbox',(SELECT count(*) FROM problem.outbox_events))::text",
        String.class,
        id,
        id,
        id);
  }

  @Test
  void versionsStayCoherentWhileTheirWriterCommitsBothChangesTogether() throws Exception {
    publishFixture(8, originalCases());
    var start = new CountDownLatch(1);
    try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
      var writer =
          executor.submit(
              () -> {
                start.await();
                for (int i = 0; i < 30; i++) {
                  int version = 7 + i % 2;
                  transaction.executeWithoutResult(
                      status -> {
                        jdbc.update(
                            "UPDATE problem.problems SET current_testcase_set_version=? WHERE id=?",
                            version,
                            id);
                        jdbc.update(
                            "UPDATE problem.problem_specs SET spec_version=? WHERE problem_id=?",
                            version + 123,
                            id);
                      });
                }
                return null;
              });
      start.countDown();
      for (int i = 0; i < 30; i++) {
        var versions = reader.loadGradingSpec(id).orElseThrow().versions();
        assertThat(versions.specVersion()).isEqualTo(versions.currentTestcaseSetVersion() + 123);
      }
      writer.get(10, TimeUnit.SECONDS);
    }
  }

  @ParameterizedTest
  @ValueSource(strings = {"SAMPLE_VISIBILITY", "SAMPLE_APPROVAL", "METADATA_APPROVAL"})
  void sqlPrivacyMutationsCannotLeakHiddenOrUnapprovedTestcases(String rule) throws Exception {
    Path file =
        Path.of(
            "../algoprep-problem-bank/src/main/java/com/algoprep/problembank/"
                + "infrastructure/persistence/JdbcGradingReadRepository.java");
    String source = Files.readString(file);
    String mutant =
        switch (rule) {
          case "SAMPLE_VISIBILITY" ->
              source.replace("visibility='SAMPLE' AND approved=true", "approved=true");
          case "SAMPLE_APPROVAL" ->
              source.replace("visibility='SAMPLE' AND approved=true", "visibility='SAMPLE'");
          default -> source.replace("t.approved=true", "true");
        };
    assertThat(mutant).isNotEqualTo(source);
    sqlCandidate(source, rule, false);
    sqlCandidate(mutant, rule, true);
    assertThat(Files.readString(file)).isEqualTo(source);
    System.out.println("MUTATION CAUGHT GRADING_" + rule + "; original GREEN; mutant RED");
  }

  private void sqlCandidate(String source, String rule, boolean mutant) throws Exception {
    String prefix = "com.algoprep.problembank.infrastructure.persistence";
    Path folder = Files.createDirectories(temporary.resolve(mutant ? "mutant" : "original"));
    Path file = folder.resolve("JdbcGradingReadRepository.java");
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
      var type = loader.loadClass(prefix + ".mutationcopy.JdbcGradingReadRepository");
      var constructor = type.getDeclaredConstructor(ObjectProvider.class);
      constructor.setAccessible(true);
      var candidate = (GradingReadRepository) constructor.newInstance(jdbcProvider);
      Runnable oracle = () -> privacyOracle(candidate, rule);
      if (mutant) {
        assertThatThrownBy(oracle::run).isInstanceOf(AssertionError.class);
      } else {
        oracle.run();
      }
    }
  }

  private void privacyOracle(GradingReadRepository candidate, String rule) {
    if (rule.equals("METADATA_APPROVAL")) {
      assertThat(candidate.findGradingSpec(id).orElseThrow().testcases())
          .extracting(row -> row.testcaseId())
          .containsExactly(sampleInline, sampleMinio, hidden);
    } else {
      assertThat(candidate.findSamples(id))
          .extracting(row -> row.testcaseId())
          .containsExactly(sampleInline, sampleMinio);
    }
  }

  private static void reason(Runnable operation, ProblemGradingReadException.Reason reason) {
    assertThatThrownBy(operation::run)
        .isInstanceOfSatisfying(
            ProblemGradingReadException.class,
            failure -> {
              assertThat(failure.reason()).isEqualTo(reason);
              assertThat(failure.getCause()).isNull();
              assertThat(failure.getSuppressed()).isEmpty();
              assertThat(failure.getMessage()).isEqualTo("Problem grading data is unavailable");
            });
  }

  @Test
  void invalidObjectReferenceIsADataFailureWithoutTransportRetries() {
    jdbc.update(
        "UPDATE problem.testcase_set_snapshots SET object_key='invalid-key' WHERE problem_id=?",
        id);
    reason(() -> reader.loadGradingSpec(id), ProblemGradingReadException.Reason.SPEC_UNAVAILABLE);
    reason(
        () -> reader.loadTestcaseSnapshot(id, 7),
        ProblemGradingReadException.Reason.SPEC_UNAVAILABLE);
  }
}
