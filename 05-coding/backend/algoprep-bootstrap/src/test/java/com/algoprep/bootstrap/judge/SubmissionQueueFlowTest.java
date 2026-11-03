package com.algoprep.bootstrap.judge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.algoprep.judge.application.dto.RecordPendingSubmissionCommand;
import com.algoprep.judge.application.ports.in.QueueSubmissionUseCase;
import com.algoprep.judge.application.ports.in.RecordPendingSubmissionUseCase;
import com.algoprep.judge.application.ports.in.ResolveQueueFailureUseCase;
import com.algoprep.judge.application.ports.out.SubmissionQueuePort;
import com.algoprep.judge.domain.exception.QueueFailureFinalizationUnavailableException;
import com.algoprep.judge.domain.model.PendingSubmission;
import com.algoprep.judge.domain.model.PendingSubmission.Owner;
import com.algoprep.judge.domain.model.PendingSubmission.Program;
import com.algoprep.judge.domain.ports.out.QueueFailureRepository;
import com.algoprep.judge.domain.ports.out.SubmissionAdmissionRepository;
import com.algoprep.problembank.application.ports.in.PublishTestcaseSnapshotUseCase;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.GetResponse;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import java.io.ByteArrayOutputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
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
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.MountableFile;
import tools.jackson.databind.json.JsonMapper;

/**
 * Real broker/PG/F2 admission; the test wrapper can model a lost confirmation after actual
 * delivery.
 */
@SpringBootTest(
    properties = {
      "spring.autoconfigure.exclude=org.springframework.boot.amqp.autoconfigure.RabbitAutoConfiguration",
      "spring.datasource.hikari.maximum-pool-size=8"
    })
@ActiveProfiles("test")
@Testcontainers
class SubmissionQueueFlowTest {
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
          .withEnv("MINIO_ROOT_USER", "test-queue-user")
          .withEnv("MINIO_ROOT_PASSWORD", "test-queue-password")
          .withCommand("server", "/data")
          .withExposedPorts(9000)
          .waitingFor(Wait.forHttp("/minio/health/ready").forPort(9000));

  @Container
  static final GenericContainer<?> RABBIT =
      new GenericContainer<>("rabbitmq:4.2.9-management-alpine")
          .withEnv("RABBITMQ_DEFAULT_USER", "test-queue-user")
          .withEnv("RABBITMQ_DEFAULT_PASS", "test-queue-password")
          .withExposedPorts(5672)
          .waitingFor(Wait.forLogMessage(".*Server startup complete.*", 1));

  @DynamicPropertySource
  static void infrastructure(DynamicPropertyRegistry properties) {
    properties.add("spring.data.redis.host", REDIS::getHost);
    properties.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
    properties.add("spring.rabbitmq.host", RABBIT::getHost);
    properties.add("spring.rabbitmq.port", () -> RABBIT.getMappedPort(5672));
    properties.add("spring.rabbitmq.username", () -> "test-queue-user");
    properties.add("spring.rabbitmq.password", () -> "test-queue-password");
    properties.add(
        "algoprep.problem-bank.storage.endpoint",
        () -> "http://" + MINIO.getHost() + ":" + MINIO.getMappedPort(9000));
    properties.add("algoprep.problem-bank.storage.access-key", () -> "test-queue-user");
    properties.add("algoprep.problem-bank.storage.secret-key", () -> "test-queue-password");
  }

  @Autowired JdbcTemplate jdbc;
  @Autowired QueueSubmissionUseCase dispatcher;
  @Autowired RecordPendingSubmissionUseCase admission;
  @Autowired ResolveQueueFailureUseCase failure;
  @Autowired QueueFailureRepository repository;
  @Autowired SubmissionAdmissionRepository admissions;
  @Autowired PublishTestcaseSnapshotUseCase snapshots;
  @Autowired PlatformTransactionManager manager;
  @Autowired ObjectProvider<JdbcClient> jdbcProvider;
  @Autowired ControlledQueue controlled;

  @Autowired
  @Qualifier("testcaseServiceClient")
  MinioClient minio;

  @TempDir Path temporary;
  private Owner owner;

  @TestConfiguration
  static class FaultConfiguration {
    @Bean
    @Primary
    ControlledQueue controlledQueue(
        @Qualifier("rabbitSubmissionQueueAdapter") SubmissionQueuePort actual, JdbcTemplate jdbc) {
      return new ControlledQueue(actual, jdbc);
    }
  }

  static class ControlledQueue implements SubmissionQueuePort {
    private final SubmissionQueuePort actual;
    private final JdbcTemplate jdbc;
    String mode = "NORMAL";

    ControlledQueue(SubmissionQueuePort actual, JdbcTemplate jdbc) {
      this.actual = actual;
      this.jdbc = jdbc;
    }

    @Override
    public boolean enqueue(UUID id) {
      assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
      assertThat(
              jdbc.queryForObject(
                  "SELECT source_code FROM judge.submissions WHERE id=?", String.class, id))
          .isEqualTo("print('private source')\r\n");
      boolean confirmed = actual.enqueue(id);
      if (mode.equals("WORKER")) {
        jdbc.update(
            "UPDATE judge.submissions SET status='COMPILING',started_grading_at=clock_timestamp() WHERE id=?",
            id);
      }
      return mode.equals("NORMAL") && confirmed;
    }
  }

  @BeforeEach
  void fixture() throws Exception {
    controlled.mode = "NORMAL";
    if (!minio.bucketExists(BucketExistsArgs.builder().bucket("algoprep-testcases").build())) {
      minio.makeBucket(MakeBucketArgs.builder().bucket("algoprep-testcases").build());
    }
    UUID actor = user("INSTRUCTOR");
    owner = new Owner(user("STUDENT"), UUID.randomUUID());
    jdbc.update(
        "INSERT INTO problem.problems(id,code,title,author_id,updated_by,level_id,time_limit_ms,memory_limit_mb,max_output_size_kb)"
            + " VALUES (?,?,'Queue fixture',?,?,'20000000-0000-4000-8000-000000000001',1000,256,64)",
        owner.problemId(),
        owner.problemId().toString(),
        actor,
        actor);
    jdbc.update(
        "INSERT INTO problem.problem_specs(id,problem_id,matching_strategy,spec_version) VALUES (?,?,'EXACT',1)",
        UUID.randomUUID(),
        owner.problemId());
    for (int i = 1; i <= 10; i++) {
      jdbc.update(
          "INSERT INTO problem.testcases(id,problem_id,visibility,position,approved,input_storage,input_inline,expected_output_storage,expected_output_inline)"
              + " VALUES (?,?,?,?,true,'INLINE','1','INLINE','2')",
          UUID.randomUUID(),
          owner.problemId(),
          i <= 2 ? "SAMPLE" : "HIDDEN",
          i);
    }
    snapshots.publishSnapshot(owner.problemId());
    jdbc.update(
        "UPDATE problem.problems SET status='PUBLISHED',published_at=clock_timestamp() WHERE id=?",
        owner.problemId());
    try (var connection = factory().newConnection();
        var channel = connection.createChannel()) {
      channel.queueDeclare("judge.submissions", true, false, false, null);
      channel.queuePurge("judge.submissions");
    }
  }

  private UUID user(String role) {
    UUID id = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO identity.users(id,email,display_name,role_id,status) SELECT ?,?,'Queue fixture',id,'ACTIVE' FROM identity.roles WHERE code=?",
        id,
        id + "@example.test",
        role);
    return id;
  }

  private RecordPendingSubmissionCommand command() {
    return new RecordPendingSubmissionCommand(
        owner, new Program("PYTHON", "STANDARD_IO", "print('private source')\r\n", "EDITOR"), null);
  }

  private static ConnectionFactory factory() {
    var factory = new ConnectionFactory();
    factory.setHost(RABBIT.getHost());
    factory.setPort(RABBIT.getMappedPort(5672));
    factory.setUsername("test-queue-user");
    factory.setPassword("test-queue-password");
    factory.setAutomaticRecoveryEnabled(false);
    return factory;
  }

  private static GetResponse message() throws Exception {
    try (var connection = factory().newConnection();
        var channel = connection.createChannel()) {
      return channel.basicGet("judge.submissions", true);
    }
  }

  private long events(UUID id) {
    return jdbc.queryForObject(
        "SELECT count(*) FROM judge.outbox_events WHERE aggregate_id=?", Long.class, id);
  }

  private String state(UUID id) {
    return jdbc.queryForObject("SELECT status FROM judge.submissions WHERE id=?", String.class, id);
  }

  @Test
  void committedPendingIsPublishedAsPersistentIdOnlyJson() throws Exception {
    var result = dispatcher.queue(command());
    assertThat(result.queueUnavailable()).isFalse();
    assertThat(result.currentStatus()).isEqualTo("PENDING");
    var delivered = message();
    assertThat(delivered).isNotNull();
    assertThat(new String(delivered.getBody(), StandardCharsets.UTF_8))
        .isEqualTo("{\"submissionId\":\"" + result.ticket().submissionId() + "\"}");
    assertThat(delivered.getProps().getDeliveryMode()).isEqualTo(2);
    assertThat(state(result.ticket().submissionId())).isEqualTo("PENDING");
    assertThat(events(result.ticket().submissionId())).isZero();
  }

  @Test
  void anOuterRollbackCannotPublishAnUncommittedAdmission() {
    var outer = new TransactionTemplate(manager);
    var id =
        outer.execute(
            status -> {
              var result = dispatcher.queue(command());
              status.setRollbackOnly();
              return result.ticket().submissionId();
            });
    assertThat(state(id)).isEqualTo("PENDING");
  }

  @Test
  void lostConfirmationEndsOnlyPendingAndPersistsTheExactGradedEnvelope() throws Exception {
    controlled.mode = "LOST";
    var result = dispatcher.queue(command());
    UUID id = result.ticket().submissionId();
    assertThat(result.queueUnavailable()).isTrue();
    assertThat(state(id)).isEqualTo("SYSTEM_ERROR");
    assertThat(
            jdbc.queryForMap(
                "SELECT failure_reason,score_ratio,finished_at,updated_at FROM judge.submissions WHERE id=?",
                id))
        .containsEntry("failure_reason", "QUEUE_UNAVAILABLE")
        .containsEntry("score_ratio", null);
    assertGraded(id, true);
    assertThat(message()).isNotNull();
    assertThat(failure.resolve(id).finalized()).isFalse();
    assertThat(events(id)).isEqualTo(1);
  }

  private void assertGraded(UUID id, boolean firstAttempt) {
    var row = jdbc.queryForMap("SELECT * FROM judge.outbox_events WHERE aggregate_id=?", id);
    var event = JsonMapper.builder().build().readTree(row.get("payload").toString());
    assertThat(row)
        .containsEntry("aggregate_type", "Submission")
        .containsEntry("event_type", "SubmissionGraded")
        .containsEntry("published_at", null)
        .containsEntry("publish_attempts", 0);
    assertThat(event.path("eventId").asText()).isEqualTo(row.get("id").toString());
    assertThat(event.path("schemaVersion").asInt()).isEqualTo(1);
    assertThat(event.path("payload").size()).isEqualTo(10);
    assertThat(event.path("payload").path("firstAttemptForProblem").asBoolean())
        .isEqualTo(firstAttempt);
    for (String field : List.of("passedCount", "totalCount", "passedTestcaseRatio")) {
      assertThat(event.path("payload").path(field).isNull()).isTrue();
    }
    var times =
        jdbc.queryForMap(
            "SELECT submitted_at,finished_at,updated_at FROM judge.submissions WHERE id=?", id);
    assertThat(Instant.parse(event.path("occurredAt").asText()))
        .isEqualTo(((java.sql.Timestamp) times.get("finished_at")).toInstant());
    assertThat(Instant.parse(event.path("payload").path("submittedAt").asText()))
        .isEqualTo(((java.sql.Timestamp) times.get("submitted_at")).toInstant());
    assertThat(times.get("finished_at"))
        .isEqualTo(times.get("updated_at"))
        .isEqualTo(row.get("created_at"));
    assertThat(event.toString())
        .doesNotContain("private source", "sourceCode", "failureReason", "stderr");
  }

  @Test
  void lostConfirmationDoesNotOverwriteAWorkerThatAlreadyStarted() throws Exception {
    controlled.mode = "WORKER";
    var result = dispatcher.queue(command());
    assertThat(result.queueUnavailable()).isFalse();
    assertThat(result.currentStatus()).isEqualTo("COMPILING");
    assertThat(state(result.ticket().submissionId())).isEqualTo("COMPILING");
    assertThat(events(result.ticket().submissionId())).isZero();
    assertThat(message()).isNotNull();
  }

  @Test
  void aStoppedBrokerFinalizesImmediatelyWithoutWaitingForSweep() throws Exception {
    assertThat(RABBIT.execInContainer("rabbitmqctl", "stop_app").getExitCode()).isZero();
    try {
      var result = dispatcher.queue(command());
      assertThat(result.queueUnavailable()).isTrue();
      assertThat(state(result.ticket().submissionId())).isEqualTo("SYSTEM_ERROR");
      assertGraded(result.ticket().submissionId(), true);
    } finally {
      assertThat(RABBIT.execInContainer("rabbitmqctl", "start_app").getExitCode()).isZero();
    }
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "COMPILING",
        "JUDGING",
        "ACCEPTED",
        "WRONG_ANSWER",
        "TIME_LIMIT_EXCEEDED",
        "RUNTIME_ERROR",
        "COMPILE_ERROR",
        "SYSTEM_ERROR"
      })
  void everyNonPendingStateIsProtected(String status) {
    UUID id = admission.recordPending(command()).submissionId();
    boolean terminal = !List.of("COMPILING", "JUDGING").contains(status);
    jdbc.update(
        "UPDATE judge.submissions SET status=?,finished_at=?,failure_reason=? WHERE id=?",
        status,
        terminal ? java.sql.Timestamp.from(admissions.databaseNow()) : null,
        status.equals("SYSTEM_ERROR") ? "WORKER_TIMEOUT" : null,
        id);
    var result = failure.resolve(id);
    assertThat(result.finalized()).isFalse();
    assertThat(result.submission().status()).isEqualTo(status);
    assertThat(events(id)).isZero();
  }

  @ParameterizedTest
  @ValueSource(strings = {"INSERT", "COMMIT"})
  void anOutboxFailureRollsTheStateBackAndCanBeRetried(String phase) {
    UUID id = admission.recordPending(command()).submissionId();
    jdbc.execute(
        "CREATE FUNCTION judge.fail_queue_event() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN RAISE EXCEPTION 'raw secret-source'; END $$");
    jdbc.execute(
        phase.equals("INSERT")
            ? "CREATE TRIGGER fail_queue_event BEFORE INSERT ON judge.outbox_events FOR EACH ROW EXECUTE FUNCTION judge.fail_queue_event()"
            : "CREATE CONSTRAINT TRIGGER fail_queue_event AFTER INSERT ON judge.outbox_events DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION judge.fail_queue_event()");
    try {
      assertThatThrownBy(() -> failure.resolve(id))
          .isInstanceOf(QueueFailureFinalizationUnavailableException.class)
          .hasMessage("Submission queue failure could not be resolved")
          .hasNoCause();
      assertThat(state(id)).isEqualTo("PENDING");
      assertThat(events(id)).isZero();
    } finally {
      jdbc.execute("DROP TRIGGER fail_queue_event ON judge.outbox_events");
      jdbc.execute("DROP FUNCTION judge.fail_queue_event()");
    }
    assertThat(failure.resolve(id).finalized()).isTrue();
    assertGraded(id, true);
  }

  @Test
  void concurrentFinalizersCommitExactlyOneTerminalEvent() throws Exception {
    UUID id = admission.recordPending(command()).submissionId();
    CountDownLatch start = new CountDownLatch(1);
    try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
      var calls = new ArrayList<java.util.concurrent.Future<Boolean>>();
      for (int i = 0; i < 6; i++) {
        calls.add(
            executor.submit(
                () -> {
                  assertThat(start.await(10, TimeUnit.SECONDS)).isTrue();
                  return failure.resolve(id).finalized();
                }));
      }
      start.countDown();
      int winners = 0;
      for (var call : calls) {
        if (call.get(20, TimeUnit.SECONDS)) {
          winners++;
        }
      }
      assertThat(winners).isEqualTo(1);
    }
    assertThat(events(id)).isEqualTo(1);
  }

  @Test
  void aWorkerCommitWhileTheFinalizerWaitsIsRecheckedByTheUpdate() throws Exception {
    UUID id = admission.recordPending(command()).submissionId();
    try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
      var pending =
          new java.util.concurrent.atomic.AtomicReference<java.util.concurrent.Future<?>>();
      new TransactionTemplate(manager)
          .executeWithoutResult(
              status -> {
                jdbc.update("UPDATE judge.submissions SET status='COMPILING' WHERE id=?", id);
                pending.set(
                    executor.submit(() -> assertThat(failure.resolve(id).finalized()).isFalse()));
                awaitRowWaiter();
              });
      pending.get().get(20, TimeUnit.SECONDS);
    }
    assertThat(state(id)).isEqualTo("COMPILING");
    assertThat(events(id)).isZero();
  }

  private void awaitRowWaiter() {
    long until = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
    while (System.nanoTime() < until) {
      jdbc.execute("SELECT pg_stat_clear_snapshot()");
      if (jdbc.queryForObject(
              "SELECT count(*) FROM pg_stat_activity WHERE wait_event_type='Lock' AND query LIKE '%UPDATE judge.submissions s SET%'",
              Integer.class)
          > 0) {
        return;
      }
      java.util.concurrent.locks.LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(20));
    }
    throw new AssertionError("Finalizer did not wait on the worker row lock");
  }

  @Test
  void firstAttemptUsesTheSubmissionTimeAndUuidTieBreakEvenForUnfinishedPredecessors() {
    Instant at = admissions.databaseNow();
    UUID earlier = new UUID(owner.problemId().getMostSignificantBits(), 1);
    UUID later = new UUID(owner.problemId().getMostSignificantBits(), 2);
    rawPending(earlier, owner, at);
    rawPending(later, owner, at);
    failure.resolve(later);
    assertGraded(later, false);
    failure.resolve(earlier);
    assertGraded(earlier, true);
  }

  private void rawPending(UUID id, Owner pair, Instant at) {
    admissions.insert(new PendingSubmission(id, pair, command().program(), 1, at));
  }

  @ParameterizedTest
  @ValueSource(strings = {"GUARD", "USER", "PROBLEM", "TIE"})
  void copiedSqlCannotOverwriteWorkerStateOrConfuseFirstAttempts(String rule) throws Exception {
    Path file =
        Path.of(
            "../algoprep-judge/src/main/java/com/algoprep/judge/infrastructure/persistence/JdbcQueueFailureRepository.java");
    String source = Files.readString(file);
    String mutant =
        switch (rule) {
          case "GUARD" -> source.replace("AND s.status='PENDING'", "");
          case "USER" -> source.replace("user_id=:userId", ":userId IS NOT NULL");
          case "PROBLEM" -> source.replace("problem_id=:problemId", ":problemId IS NOT NULL");
          default ->
              source.replace(
                  "(submitted_at,id) < (:submittedAt,:id)",
                  "submitted_at < :submittedAt AND :id IS NOT NULL");
        };
    assertThat(mutant).isNotEqualTo(source);
    UUID id = new UUID(owner.problemId().getMostSignificantBits(), 20);
    Instant at = admissions.databaseNow();
    rawPending(id, owner, at);
    seedSqlBranch(rule, at, id);
    sqlCandidate(source, rule, id, false);
    sqlCandidate(mutant, rule, id, true);
    assertThat(Files.readString(file)).isEqualTo(source);
    System.out.println("MUTATION CAUGHT QUEUE_FINAL_SQL_" + rule + "; original GREEN; mutant RED");
  }

  private void seedSqlBranch(String rule, Instant at, UUID id) {
    if (rule.equals("GUARD")) {
      jdbc.update("UPDATE judge.submissions SET status='COMPILING' WHERE id=?", id);
    } else {
      Owner pair =
          rule.equals("USER")
              ? new Owner(UUID.randomUUID(), owner.problemId())
              : rule.equals("PROBLEM") ? new Owner(owner.userId(), UUID.randomUUID()) : owner;
      rawPending(
          new UUID(owner.problemId().getMostSignificantBits(), 10),
          pair,
          rule.equals("TIE") ? at : at.minusSeconds(1));
    }
  }

  private void sqlCandidate(String source, String rule, UUID id, boolean mutant) throws Exception {
    Path folder = compileSql(source, mutant);
    try (var loader =
        new URLClassLoader(new URL[] {folder.toUri().toURL()}, getClass().getClassLoader())) {
      var constructor =
          loader
              .loadClass(
                  "com.algoprep.judge.infrastructure.persistence.mutationcopy.JdbcQueueFailureRepository")
              .getDeclaredConstructor(ObjectProvider.class);
      constructor.setAccessible(true);
      var candidate = (QueueFailureRepository) constructor.newInstance(jdbcProvider);
      Runnable oracle =
          () ->
              new TransactionTemplate(manager)
                  .executeWithoutResult(
                      status -> {
                        status.setRollbackOnly();
                        if (rule.equals("GUARD")) {
                          assertThat(candidate.finishPending(id)).isEmpty();
                        } else {
                          assertThat(candidate.firstAttempt(candidate.find(id).orElseThrow()))
                              .isEqualTo(!rule.equals("TIE"));
                        }
                      });
      if (mutant) {
        assertThatThrownBy(oracle::run).isInstanceOf(AssertionError.class);
      } else {
        oracle.run();
      }
    }
  }

  private Path compileSql(String source, boolean mutant) throws Exception {
    Path folder = Files.createDirectories(temporary.resolve(mutant ? "mutant" : "original"));
    Path file = folder.resolve("JdbcQueueFailureRepository.java");
    String prefix = "package com.algoprep.judge.infrastructure.persistence;";
    Files.writeString(file, source.replace(prefix, prefix.replace(";", ".mutationcopy;")));
    var errors = new ByteArrayOutputStream();
    int exit =
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
    assertThat(exit).withFailMessage(errors.toString(StandardCharsets.UTF_8)).isZero();
    return folder;
  }
}
