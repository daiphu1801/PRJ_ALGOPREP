package com.algoprep.bootstrap.problembank;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.algoprep.common.exception.AlgoPrepException;
import com.algoprep.common.functionspec.Language;
import com.algoprep.common.functionspec.MatchingStrategy;
import com.algoprep.common.functionspec.TypeKind;
import com.algoprep.harness.application.dto.GetStarterCodeQuery;
import com.algoprep.harness.application.dto.PrepareSubmissionCommand;
import com.algoprep.harness.application.dto.SubmissionModel;
import com.algoprep.harness.application.ports.in.GetStarterCodeUseCase;
import com.algoprep.harness.application.ports.in.PrepareSubmissionUseCase;
import com.algoprep.harness.application.ports.out.ProblemSpecReaderPort;
import com.algoprep.harness.application.ports.out.StarterCodeAccessPort;
import com.algoprep.problembank.application.dto.SaveProblemSpecCommand;
import com.algoprep.problembank.application.ports.in.SaveProblemSpecUseCase;
import com.algoprep.problembank.domain.ports.out.HarnessProblemReadRepository;
import java.io.ByteArrayOutputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.UUID;
import javax.tools.ToolProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.MountableFile;
import tools.jackson.databind.json.JsonMapper;

/**
 * Actual F2 save/read/permissions -> bootstrap -> F3 P1/P2. No fake F2 reader or sandbox claims.
 */
@SpringBootTest(
    properties =
        "spring.autoconfigure.exclude=org.springframework.boot.amqp.autoconfigure.RabbitAutoConfiguration")
@ActiveProfiles("test")
@Testcontainers
class HarnessProblemReadFlowTest {
  private static final JsonMapper JSON = JsonMapper.builder().build();

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

  @DynamicPropertySource
  static void infrastructure(DynamicPropertyRegistry properties) {
    properties.add("spring.data.redis.host", REDIS::getHost);
    properties.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
  }

  @Autowired JdbcTemplate jdbc;
  @Autowired StringRedisTemplate redis;
  @Autowired SaveProblemSpecUseCase save;
  @Autowired GetStarterCodeUseCase starter;
  @Autowired PrepareSubmissionUseCase prepare;
  @Autowired ProblemSpecReaderPort reader;
  @Autowired StarterCodeAccessPort access;
  @Autowired ObjectProvider<JdbcClient> jdbcProvider;
  @TempDir Path temporary;
  private UUID id;
  private UUID author;

  @BeforeEach
  void fixture() {
    id = UUID.randomUUID();
    author = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO identity.users(id,email,display_name,role_id,status)"
            + " SELECT ?,?,'Read actor',id,'ACTIVE' FROM identity.roles WHERE code='INSTRUCTOR'",
        author,
        author + "@example.test");
    jdbc.update(
        "INSERT INTO problem.problems(id,code,title,author_id,updated_by,level_id,"
            + "time_limit_ms,memory_limit_mb,max_output_size_kb)"
            + " VALUES (?,?,'Read fixture',?,?,'20000000-0000-4000-8000-000000000001',1000,256,64)",
        id,
        id.toString(),
        author,
        author);
  }

  private Instant timestamp() {
    return jdbc.queryForObject(
            "SELECT updated_at FROM problem.problems WHERE id=?", java.sql.Timestamp.class, id)
        .toInstant();
  }

  private void save(String name, String type) {
    var body =
        JSON.createObjectNode()
            .put("functionName", name)
            .put("matchingStrategy", "EPSILON")
            .put("epsilonValue", new java.math.BigDecimal("0.000000000001"))
            .put("stdinFormatMd", "raw stdin")
            .put("stdoutFormatMd", "raw stdout");
    body.set("returnType", JSON.readTree(type));
    body.set("parameters", JSON.readTree("[{\"name\":\"value\",\"type\":{\"kind\":\"INT\"}}]"));
    body.set("nameOverrides", JSON.readTree("{\"JAVA\":\"echoJava\"}"));
    save.save(new SaveProblemSpecCommand(id, author, false, timestamp(), body));
  }

  private PrepareSubmissionCommand command(
      Language language, SubmissionModel model, Integer version) {
    return new PrepareSubmissionCommand(
        id, language, model, "user-source\r\n", version, "trace-only");
  }

  private GetStarterCodeQuery query(UUID actor, String category, SubmissionModel model) {
    return new GetStarterCodeQuery(id, Language.PYTHON, model, actor, category);
  }

  @ParameterizedTest
  @EnumSource(Language.class)
  void preparesBothModelsFromActualSavedJsonAndKeepsMatchingVersionAtomic(Language language) {
    save("echo", "{\"kind\":\"INT\"}");
    for (var model : SubmissionModel.values()) {
      var started =
          starter.getStarterCode(
              new GetStarterCodeQuery(id, language, model, author, "INSTRUCTOR"));
      assertThat(started.specVersion()).isEqualTo(1);
      var result = prepare.prepare(command(language, model, 1));
      assertThat(result.specVersion()).isEqualTo(1);
      assertThat(result.matchSpec().matchingStrategy()).isEqualTo(MatchingStrategy.EPSILON);
      assertThat(result.matchSpec().epsilonValue()).isEqualByComparingTo("0.000000000001");
      assertThat(result.harnessInputErrorExitCode()).isEqualTo(90);
      assertThat(result.payload().body().toString()).doesNotContain("trace-only", "raw stdin");
      assertThat(prepare.prepare(command(language, model, 1))).isEqualTo(result);
      if (model == SubmissionModel.STANDARD_IO) {
        assertThat(result.matchSpec().returnType()).isNull();
      } else if (language == Language.JAVA) {
        assertThat(started.sourceCode()).contains("echoJava");
        assertThat(result.signatureText()).contains("echoJava");
      }
    }
  }

  @Test
  void liveAccessHidesDraftAndDeletedSpecEvenIfRedisContainsCachedData() {
    save("echo", "{\"kind\":\"INT\"}");
    UUID stranger = UUID.randomUUID();
    redis.opsForValue().set("problem:spec:" + id, "stale-private-spec");
    assertThat(access.canRead(id, author, "INSTRUCTOR")).isTrue();
    assertThat(access.canRead(id, stranger, "ADMIN")).isTrue();
    assertCode(
        () -> starter.getStarterCode(query(stranger, "INSTRUCTOR", SubmissionModel.STANDARD_IO)),
        "HRN-001");
    assertCode(
        () -> starter.getStarterCode(query(stranger, "STUDENT", SubmissionModel.STANDARD_IO)),
        "HRN-001");
    jdbc.update("UPDATE problem.problems SET status='PUBLISHED' WHERE id=?", id);
    assertThat(
            starter
                .getStarterCode(query(stranger, "STUDENT", SubmissionModel.STANDARD_IO))
                .specVersion())
        .isEqualTo(1);
    jdbc.update("UPDATE problem.problems SET status='UNPUBLISHED' WHERE id=?", id);
    assertCode(
        () -> starter.getStarterCode(query(stranger, "STUDENT", SubmissionModel.STANDARD_IO)),
        "HRN-001");
    jdbc.update("UPDATE problem.problems SET deleted=true WHERE id=?", id);
    assertThat(access.canRead(id, author, "INSTRUCTOR")).isFalse();
    assertCode(
        () -> starter.getStarterCode(query(stranger, "ADMIN", SubmissionModel.STANDARD_IO)),
        "HRN-001");
    assertThat(reader.findSpec(id))
        .isPresent(); // Trusted pending-work readers are distinct from P1 access.
  }

  @Test
  void missingRowAndDeniedDraftProduceTheSameCodeAndMessage() {
    var absent =
        assertCode(
            () -> starter.getStarterCode(query(author, "INSTRUCTOR", SubmissionModel.STANDARD_IO)),
            "HRN-001");
    save("echo", "{\"kind\":\"INT\"}");
    var denied =
        assertCode(
            () ->
                starter.getStarterCode(
                    query(UUID.randomUUID(), "STUDENT", SubmissionModel.STANDARD_IO)),
            "HRN-001");
    assertThat(denied.getMessage()).isEqualTo(absent.getMessage());
    assertThat(access.canRead(id, null, "ADMIN")).isFalse();
    assertThat(access.canRead(UUID.randomUUID(), author, "ADMIN")).isFalse();
  }

  @Test
  void freshVersionBypassesStaleCacheAndRejectsPinnedOlderVersion() {
    save("echo", "{\"kind\":\"INT\"}");
    redis.opsForValue().set("problem:spec:" + id, "stale-private-spec");
    assertThat(
            prepare
                .prepare(command(Language.PYTHON, SubmissionModel.FUNCTION_WRAPPER, 1))
                .specVersion())
        .isEqualTo(1);
    assertThat(redis.opsForValue().get("problem:spec:" + id)).isEqualTo("stale-private-spec");
    save("nextEcho", "{\"kind\":\"INT\"}");
    assertCode(
        () -> prepare.prepare(command(Language.PYTHON, SubmissionModel.FUNCTION_WRAPPER, 1)),
        "HRN-002");
    var fresh = prepare.prepare(command(Language.PYTHON, SubmissionModel.FUNCTION_WRAPPER, null));
    assertThat(fresh.specVersion()).isEqualTo(2);
    assertThat(fresh.signatureText()).contains("next_echo");
  }

  @Test
  void unknownKindsStayRawInF2ButBlockWrapperWhileStandardIoWorks() {
    save("echo", "{\"kind\":\"BIG_DECIMAL\"}");
    assertThat(reader.findSpec(id).orElseThrow().signature().returnType().kind())
        .isEqualTo(TypeKind.OTHER);
    assertThat(
            jdbc.queryForObject(
                "SELECT return_type->>'kind' FROM problem.problem_specs WHERE problem_id=?",
                String.class,
                id))
        .isEqualTo("BIG_DECIMAL");
    assertCode(
        () -> prepare.prepare(command(Language.PYTHON, SubmissionModel.FUNCTION_WRAPPER, 1)),
        "HRN-004");
    assertThat(
            prepare.prepare(command(Language.PYTHON, SubmissionModel.STANDARD_IO, 1)).specVersion())
        .isEqualTo(1);
    jdbc.update("UPDATE problem.problems SET function_wrapper_supported=true WHERE id=?", id);
    assertCode(
        () -> prepare.prepare(command(Language.PYTHON, SubmissionModel.FUNCTION_WRAPPER, 1)),
        "HRN-006");
  }

  @Test
  void absentSignatureAndCorruptJsonAreNotConfusedWithInfrastructureErrors() {
    var body = JSON.readTree("{\"stdinFormatMd\":\"in\",\"stdoutFormatMd\":\"out\"}");
    save.save(new SaveProblemSpecCommand(id, author, false, timestamp(), body));
    assertCode(
        () -> prepare.prepare(command(Language.PYTHON, SubmissionModel.FUNCTION_WRAPPER, 1)),
        "HRN-005");
    assertThat(
            prepare
                .prepare(command(Language.PYTHON, SubmissionModel.STANDARD_IO, 1))
                .signatureText())
        .isNull();
    save("echo", "{\"kind\":\"INT\"}");
    jdbc.update(
        "UPDATE problem.problem_specs SET return_type='{\"kind\":123}'::jsonb WHERE problem_id=?",
        id);
    assertCode(() -> reader.findSpec(id), "HRN-006");
  }

  @Test
  void readDoesNotChangeDatabaseTimestampVersionsOutboxOrCache() {
    save("echo", "{\"kind\":\"INT\"}");
    Instant before = timestamp();
    Long events =
        jdbc.queryForObject(
            "SELECT count(*) FROM problem.outbox_events WHERE aggregate_id=?", Long.class, id);
    for (int i = 0; i < 2; i++) {
      starter.getStarterCode(query(author, "INSTRUCTOR", SubmissionModel.FUNCTION_WRAPPER));
      prepare.prepare(command(Language.PYTHON, SubmissionModel.FUNCTION_WRAPPER, 1));
    }
    assertThat(timestamp()).isEqualTo(before);
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM problem.outbox_events WHERE aggregate_id=?", Long.class, id))
        .isEqualTo(events);
    assertThat(reader.findSpec(id).orElseThrow().specVersion()).isEqualTo(1);
  }

  @Test
  void missingStorageTableBecomesSanitizedUnavailableAndRecovers() {
    save("echo", "{\"kind\":\"INT\"}");
    jdbc.execute("ALTER TABLE problem.problem_specs RENAME TO unavailable_specs");
    try {
      assertCode(() -> reader.findSpec(id), "HRN-003");
    } finally {
      jdbc.execute("ALTER TABLE problem.unavailable_specs RENAME TO problem_specs");
    }
    assertThat(reader.findSpec(id)).isPresent();
  }

  private AlgoPrepException assertCode(Runnable action, String code) {
    var captured = new java.util.concurrent.atomic.AtomicReference<AlgoPrepException>();
    assertThatThrownBy(action::run)
        .isInstanceOfSatisfying(
            AlgoPrepException.class,
            error -> {
              assertThat(error.errorCode()).isEqualTo(code);
              assertThat(error.getCause()).isNull();
              assertThat(error.getMessage())
                  .doesNotContain("123", "SELECT", "private", "unavailable_specs");
              captured.set(error);
            });
    return captured.get();
  }

  @ParameterizedTest
  @ValueSource(strings = {"DRAFT_PRIVACY", "DELETED_GUARD"})
  void copiedSqlMutationsCannotBypassLiveAccessChecks(String rule) throws Exception {
    boolean deleted = rule.equals("DELETED_GUARD");
    if (deleted) {
      jdbc.update("UPDATE problem.problems SET deleted=true WHERE id=?", id);
    }
    Path sourceFile =
        Path.of(
            "../algoprep-problem-bank/src/main/java/com/algoprep/problembank/"
                + "infrastructure/persistence/JdbcHarnessProblemReadRepository.java");
    String source = Files.readString(sourceFile);
    String mutant =
        deleted
            ? source.replace("AND NOT deleted", "")
            : source.replace("status='PUBLISHED' OR author_id=:caller OR :admin", "true");
    assertThat(mutant).isNotEqualTo(source);
    sqlCandidate(source, deleted, false);
    sqlCandidate(mutant, deleted, true);
    assertThat(Files.readString(sourceFile)).isEqualTo(source);
    System.out.println(
        "MUTATION CAUGHT READ_SQL_"
            + rule
            + "; original GREEN; mutant oracle RED; source unchanged");
  }

  private void sqlCandidate(String source, boolean deleted, boolean mutant) throws Exception {
    String prefix = "com.algoprep.problembank.infrastructure.persistence";
    Path folder = Files.createDirectories(temporary.resolve(mutant ? "mutant" : "original"));
    Path file = folder.resolve("JdbcHarnessProblemReadRepository.java");
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
      var type = loader.loadClass(prefix + ".mutationcopy.JdbcHarnessProblemReadRepository");
      var constructor = type.getDeclaredConstructor(ObjectProvider.class);
      constructor.setAccessible(true);
      var repository = (HarnessProblemReadRepository) constructor.newInstance(jdbcProvider);
      Runnable oracle =
          () ->
              assertThat(
                      repository.canRead(id, UUID.randomUUID(), deleted ? "ADMIN" : "INSTRUCTOR"))
                  .isFalse();
      if (mutant) {
        assertThatThrownBy(oracle::run).isInstanceOf(AssertionError.class);
      } else {
        oracle.run();
      }
    }
  }
}
