package com.algoprep.bootstrap.judge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.algoprep.judge.application.dto.RecordPendingSubmissionCommand;
import com.algoprep.judge.application.ports.in.RecordPendingSubmissionUseCase;
import com.algoprep.judge.application.ports.out.ProblemGradingSpecPort;
import com.algoprep.judge.domain.exception.SubmissionAdmissionException;
import com.algoprep.judge.domain.exception.SubmissionAdmissionUnavailableException;
import com.algoprep.judge.domain.model.PendingSubmission;
import com.algoprep.judge.domain.model.PendingSubmission.Owner;
import com.algoprep.judge.domain.model.PendingSubmission.Program;
import com.algoprep.judge.domain.ports.out.SubmissionAdmissionRepository;
import com.algoprep.problembank.application.ports.in.PublishTestcaseSnapshotUseCase;
import com.algoprep.problembank.application.ports.out.TestcaseObjectStorePort;
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
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
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
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.MountableFile;

/**
 * Trusted kernel with real F2/MinIO reads and PostgreSQL admission; queue and HTTP are separate.
 */
@SpringBootTest(
    properties = {
      "spring.autoconfigure.exclude=org.springframework.boot.amqp.autoconfigure.RabbitAutoConfiguration",
      "spring.datasource.hikari.maximum-pool-size=6"
    })
@ActiveProfiles("test")
@Testcontainers
class PendingSubmissionFlowTest {
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
          .withEnv("MINIO_ROOT_USER", "test-admission-user")
          .withEnv("MINIO_ROOT_PASSWORD", "test-admission-password")
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
    properties.add("algoprep.problem-bank.storage.access-key", () -> "test-admission-user");
    properties.add("algoprep.problem-bank.storage.secret-key", () -> "test-admission-password");
  }

  @Autowired JdbcTemplate jdbc;
  @Autowired RecordPendingSubmissionUseCase admission;
  @Autowired SubmissionAdmissionRepository repository;
  @Autowired PublishTestcaseSnapshotUseCase snapshots;
  @Autowired ProblemGradingSpecPort problems;
  @Autowired TestcaseObjectStorePort storage;
  @Autowired PlatformTransactionManager manager;
  @Autowired ObjectProvider<JdbcClient> jdbcProvider;

  @Autowired
  @Qualifier("testcaseServiceClient")
  MinioClient minio;

  @TempDir Path temporary;
  private Owner owner;
  private UUID actor;

  @BeforeEach
  void fixture() throws Exception {
    if (!minio.bucketExists(BucketExistsArgs.builder().bucket("algoprep-testcases").build())) {
      minio.makeBucket(MakeBucketArgs.builder().bucket("algoprep-testcases").build());
    }
    jdbc.update("UPDATE judge.language_configs SET enabled=true");
    jdbc.update("UPDATE judge.cluster_settings SET queue_paused=false");
    actor = user("INSTRUCTOR");
    owner = new Owner(user("STUDENT"), UUID.randomUUID());
    seedProblem(owner.problemId());
  }

  private UUID user(String role) {
    UUID id = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO identity.users(id,email,display_name,role_id,status) SELECT ?,?,'Admission fixture',id,'ACTIVE' FROM identity.roles WHERE code=?",
        id,
        id + "@example.test",
        role);
    return id;
  }

  private void seedProblem(UUID id) {
    jdbc.update(
        "INSERT INTO problem.problems(id,code,title,author_id,updated_by,level_id,time_limit_ms,memory_limit_mb,max_output_size_kb)"
            + " VALUES (?,?,'Admission fixture',?,?,'20000000-0000-4000-8000-000000000001',1000,256,64)",
        id,
        id.toString(),
        actor,
        actor);
    jdbc.update(
        "INSERT INTO problem.problem_specs(id,problem_id,matching_strategy,spec_version) VALUES (?,?,'EXACT',7)",
        UUID.randomUUID(),
        id);
    for (int i = 1; i <= 10; i++) {
      jdbc.update(
          "INSERT INTO problem.testcases(id,problem_id,visibility,position,approved,input_storage,input_inline,expected_output_storage,expected_output_inline)"
              + " VALUES (?,?,?,?,true,'INLINE',?,'INLINE',?)",
          UUID.randomUUID(),
          id,
          i <= 2 ? "SAMPLE" : "HIDDEN",
          i,
          i + "\n",
          "đáp án\r\n");
    }
    snapshots.publishSnapshot(id);
    jdbc.update(
        "UPDATE problem.problems SET status='PUBLISHED',published_at=clock_timestamp() WHERE id=?",
        id);
  }

  private RecordPendingSubmissionCommand command() {
    return new RecordPendingSubmissionCommand(
        owner, new Program("PYTHON", "STANDARD_IO", " print('đáp án')\r\n", "EDITOR"), null);
  }

  private long count(Owner pair) {
    return jdbc.queryForObject(
        "SELECT count(*) FROM judge.submissions WHERE user_id=? AND problem_id=?",
        Long.class,
        pair.userId(),
        pair.problemId());
  }

  private Map<String, Object> row(UUID id) {
    return jdbc.queryForMap("SELECT * FROM judge.submissions WHERE id=?", id);
  }

  private void quota(int limit) {
    jdbc.update(
        "UPDATE problem.problems SET max_submissions_per_hour=? WHERE id=?",
        limit,
        owner.problemId());
  }

  @Test
  void admissionCommitsPendingWithoutWorkerFieldsOrEventsAndRetainsTheOriginalSource() {
    var ticket = admission.recordPending(command());
    Map<String, Object> data = row(ticket.submissionId());
    assertThat(ticket.status()).isEqualTo("PENDING");
    assertThat(ticket.totalCountHint()).isEqualTo(10);
    assertThat(data)
        .containsEntry("user_id", owner.userId())
        .containsEntry("problem_id", owner.problemId())
        .containsEntry("source_code", command().program().sourceCode())
        .containsEntry("source_input_method", "EDITOR")
        .containsEntry("passed_testcase_count", 0)
        .containsEntry("testcase_set_version_at_grading", 1);
    for (String field :
        List.of(
            "spec_version",
            "total_testcase_count",
            "score_ratio",
            "failure_reason",
            "started_grading_at",
            "finished_at",
            "runtime_ms",
            "memory_kb")) {
      assertThat(data.get(field)).as(field).isNull();
    }
    assertThat(data.get("submitted_at")).isEqualTo(data.get("updated_at"));
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM judge.outbox_events WHERE aggregate_id=?",
                Long.class,
                ticket.submissionId()))
        .isZero();
    assertThat(count(owner)).isEqualTo(1);
  }

  @Test
  void repeatedCallsAreDistinctAndPauseDoesNotRejectNewSubmissions() {
    jdbc.update("UPDATE judge.cluster_settings SET queue_paused=true");
    var first = admission.recordPending(command());
    var second = admission.recordPending(command());
    assertThat(first.submissionId()).isNotEqualTo(second.submissionId());
    assertThat(count(owner)).isEqualTo(2);
  }

  @Test
  void hourlyDefaultAllows60ThenRejects61WithNoInsert() {
    seedHistory(owner, 59, "PENDING");
    admission.recordPending(command());
    rejected(() -> admission.recordPending(command()), "JDG-106");
    assertThat(count(owner)).isEqualTo(60);
  }

  @Test
  void customQuotaCountsSystemErrorsButDoesNotAffectOtherUsersOrProblems() {
    quota(5);
    seedHistory(owner, 4, "SYSTEM_ERROR");
    admission.recordPending(command());
    rejected(() -> admission.recordPending(command()), "JDG-106");
    Owner anotherUser = new Owner(user("STUDENT"), owner.problemId());
    var original = command();
    assertThat(
            admission
                .recordPending(
                    new RecordPendingSubmissionCommand(anotherUser, original.program(), null))
                .status())
        .isEqualTo("PENDING");
    UUID otherProblem = UUID.randomUUID();
    seedProblem(otherProblem);
    Owner anotherProblem = new Owner(owner.userId(), otherProblem);
    admission.recordPending(
        new RecordPendingSubmissionCommand(anotherProblem, original.program(), null));
    assertThat(count(owner)).isEqualTo(5);
  }

  private void seedHistory(Owner pair, int count, String status) {
    for (int i = 0; i < count; i++) {
      Instant at = repository.databaseNow().minusSeconds(1800);
      repository.insert(new PendingSubmission(UUID.randomUUID(), pair, command().program(), 1, at));
    }
    if (status.equals("SYSTEM_ERROR")) {
      jdbc.update(
          "UPDATE judge.submissions SET status='SYSTEM_ERROR',failure_reason='WORKER_TIMEOUT',finished_at=clock_timestamp() WHERE user_id=? AND problem_id=?",
          pair.userId(),
          pair.problemId());
    }
  }

  @Test
  void strictOneHourWindowIncludesOnlyNewerRowsOfTheExactPair() {
    Instant at = repository.databaseNow();
    for (long offset : new long[] {-1, 0, 1}) {
      repository.insert(
          new PendingSubmission(
              UUID.randomUUID(),
              owner,
              command().program(),
              1,
              at.minus(1, ChronoUnit.HOURS).plus(offset, ChronoUnit.MICROS)));
    }
    assertThat(repository.countRecent(owner, at)).isEqualTo(1);
  }

  @Test
  void twoConcurrentCallsForTheLastSlotHaveExactlyOneWinner() throws Exception {
    quota(1);
    CountDownLatch start = new CountDownLatch(1);
    try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
      Callable<String> attempt =
          () -> {
            assertThat(start.await(10, TimeUnit.SECONDS)).isTrue();
            try {
              admission.recordPending(command());
              return "PENDING";
            } catch (SubmissionAdmissionException e) {
              return e.errorCode();
            }
          };
      var first = executor.submit(attempt);
      var second = executor.submit(attempt);
      start.countDown();
      assertThat(List.of(first.get(20, TimeUnit.SECONDS), second.get(20, TimeUnit.SECONDS)))
          .containsExactlyInAnyOrder("PENDING", "JDG-106");
    }
    assertThat(count(owner)).isEqualTo(1);
  }

  @Test
  void aWaitingAdmissionSeesTheCommittedInsertAfterAcquiringTheLock() throws Exception {
    quota(1);
    try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
      var future =
          new java.util.concurrent.atomic.AtomicReference<java.util.concurrent.Future<?>>();
      new TransactionTemplate(manager)
          .executeWithoutResult(
              status -> {
                repository.lockPair(owner);
                future.set(
                    executor.submit(
                        () -> rejected(() -> admission.recordPending(command()), "JDG-106")));
                awaitAdvisoryWaiter();
                repository.insert(
                    new PendingSubmission(
                        UUID.randomUUID(),
                        owner,
                        command().program(),
                        1,
                        repository.databaseNow()));
              });
      future.get().get(20, TimeUnit.SECONDS);
    }
    assertThat(count(owner)).isEqualTo(1);
  }

  private void awaitAdvisoryWaiter() {
    long until = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
    while (System.nanoTime() < until) {
      if (jdbc.queryForObject(
              "SELECT count(*) FROM pg_stat_activity WHERE wait_event='advisory'", Integer.class)
          > 0) {
        return;
      }
      java.util.concurrent.locks.LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(20));
    }
    throw new AssertionError("Admission did not wait on its pair lock");
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "MISSING",
        "WITHDRAWN",
        "DELETED",
        "NO_TESTCASES",
        "NO_SNAPSHOT",
        "NO_OBJECT",
        "LANGUAGE",
        "WRAPPER"
      })
  void problemAndLanguageGuardsRejectWithoutCreatingRows(String branch) {
    String expected = "JDG-101";
    switch (branch) {
      case "MISSING" -> owner = new Owner(owner.userId(), UUID.randomUUID());
      case "WITHDRAWN" ->
          jdbc.update(
              "UPDATE problem.problems SET status='UNPUBLISHED' WHERE id=?", owner.problemId());
      case "DELETED" ->
          jdbc.update(
              "UPDATE problem.problems SET deleted=true,status='UNPUBLISHED' WHERE id=?",
              owner.problemId());
      case "NO_TESTCASES" -> {
        jdbc.update(
            "UPDATE problem.testcases SET approved=false WHERE problem_id=?", owner.problemId());
        expected = "JDG-104";
      }
      case "NO_SNAPSHOT" -> {
        jdbc.update(
            "DELETE FROM problem.testcase_set_snapshots WHERE problem_id=?", owner.problemId());
        expected = "JDG-504";
      }
      case "NO_OBJECT" -> {
        storage.delete(
            jdbc.queryForObject(
                "SELECT object_key FROM problem.testcase_set_snapshots WHERE problem_id=?",
                String.class,
                owner.problemId()));
        expected = "JDG-504";
      }
      case "LANGUAGE" -> {
        jdbc.update("UPDATE judge.language_configs SET enabled=false WHERE language='PYTHON'");
        expected = "JDG-102";
      }
      case "WRAPPER" -> {
        jdbc.update(
            "UPDATE problem.problems SET function_wrapper_supported=false WHERE id=?",
            owner.problemId());
        expected = "JDG-103";
      }
      default -> throw new AssertionError(branch);
    }
    var cmd =
        branch.equals("WRAPPER")
            ? new RecordPendingSubmissionCommand(
                owner, new Program("PYTHON", "FUNCTION_WRAPPER", "pass", "EDITOR"), null)
            : command();
    String code = expected;
    rejected(() -> admission.recordPending(cmd), code);
    assertThat(count(owner)).isZero();
  }

  @Test
  void uploadedUtf8SourceAtTheLimitIsStoredVerbatimAndOversizeIsRejected() {
    var program = new Program("python", "standard_io", "đ".repeat(32768), "file_upload");
    var ticket =
        admission.recordPending(new RecordPendingSubmissionCommand(owner, program, "Main.PY"));
    assertThat(row(ticket.submissionId()))
        .containsEntry("source_code", program.sourceCode())
        .containsEntry("source_input_method", "FILE_UPLOAD");
    rejected(
        () ->
            admission.recordPending(
                new RecordPendingSubmissionCommand(
                    owner,
                    new Program("PYTHON", "STANDARD_IO", "đ".repeat(32768) + "a", "FILE_UPLOAD"),
                    "Main.py")),
        "JDG-V02");
    rejected(
        () ->
            admission.recordPending(
                new RecordPendingSubmissionCommand(owner, program, "Main.java")),
        "JDG-V06");
    assertThat(count(owner)).isEqualTo(1);
  }

  @Test
  void pinnedVersionSurvivesANewSnapshotAndWithdrawal() {
    var ticket = admission.recordPending(command());
    jdbc.update(
        "UPDATE problem.testcases SET expected_output_inline='new' WHERE problem_id=? AND position=1",
        owner.problemId());
    assertThat(snapshots.publishSnapshot(owner.problemId()).version()).isEqualTo(2);
    jdbc.update("UPDATE problem.problems SET status='UNPUBLISHED' WHERE id=?", owner.problemId());
    assertThat(row(ticket.submissionId())).containsEntry("testcase_set_version_at_grading", 1);
    assertThat(
            problems.loadTestcaseSnapshot(owner.problemId(), 1).testcases().get(0).expectedOutput())
        .containsExactly("đáp án\r\n".getBytes(StandardCharsets.UTF_8));
    rejected(() -> admission.recordPending(command()), "JDG-101");
  }

  @Test
  void failedInsertRollsBackAndReleasesThePairLockWithoutLeakingSql() {
    jdbc.execute(
        "CREATE FUNCTION judge.fail_admission() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN RAISE EXCEPTION 'secret-source'; END $$");
    jdbc.execute(
        "CREATE TRIGGER fail_admission BEFORE INSERT ON judge.submissions FOR EACH ROW EXECUTE FUNCTION judge.fail_admission()");
    try {
      assertThatThrownBy(() -> admission.recordPending(command()))
          .isInstanceOf(SubmissionAdmissionUnavailableException.class)
          .hasMessage("Submission admission is unavailable")
          .hasNoCause();
      assertThat(count(owner)).isZero();
    } finally {
      jdbc.execute("DROP TRIGGER fail_admission ON judge.submissions");
      jdbc.execute("DROP FUNCTION judge.fail_admission()");
    }
    assertThat(admission.recordPending(command()).status()).isEqualTo("PENDING");
  }

  @Test
  void independentAdmissionCommitsEvenWhenAnOuterTransactionRollsBack() {
    var outer = new TransactionTemplate(manager);
    outer.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
    outer.executeWithoutResult(
        status -> {
          assertThat(count(owner)).isZero();
          admission.recordPending(command());
          status.setRollbackOnly();
        });
    assertThat(count(owner)).isEqualTo(1);
  }

  @Test
  void aDeferredCommitFailureIsAlsoSanitizedAndLeavesNoSubmission() {
    jdbc.execute(
        "CREATE FUNCTION judge.fail_admission_commit() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN RAISE EXCEPTION 'secret-source'; END $$");
    jdbc.execute(
        "CREATE CONSTRAINT TRIGGER fail_admission_commit AFTER INSERT ON judge.submissions DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION judge.fail_admission_commit()");
    try {
      assertThatThrownBy(() -> admission.recordPending(command()))
          .isInstanceOf(SubmissionAdmissionUnavailableException.class)
          .hasMessage("Submission admission is unavailable")
          .hasNoCause();
      assertThat(count(owner)).isZero();
    } finally {
      jdbc.execute("DROP TRIGGER fail_admission_commit ON judge.submissions");
      jdbc.execute("DROP FUNCTION judge.fail_admission_commit()");
    }
    assertThat(admission.recordPending(command()).status()).isEqualTo("PENDING");
  }

  @ParameterizedTest
  @ValueSource(strings = {"USER", "PROBLEM", "WINDOW", "LOCK"})
  void copiedSqlMustPreservePairScopeStrictWindowAndTransactionLock(String rule) throws Exception {
    Path file =
        Path.of(
            "../algoprep-judge/src/main/java/com/algoprep/judge/infrastructure/persistence/JdbcSubmissionAdmissionRepository.java");
    String source = Files.readString(file);
    String mutant =
        switch (rule) {
          case "USER" -> source.replace("user_id=:userId", ":userId IS NOT NULL");
          case "PROBLEM" -> source.replace("problem_id=:problemId", ":problemId IS NOT NULL");
          case "WINDOW" -> source.replace("submitted_at > :at", "submitted_at >= :at");
          default ->
              source.replace(
                  "SELECT pg_advisory_xact_lock(hashtextextended(:pair, 0))",
                  "SELECT true FROM (SELECT hashtextextended(:pair, 0)) ignored");
        };
    assertThat(mutant).isNotEqualTo(source);
    Instant at = repository.databaseNow();
    Owner target =
        rule.equals("USER")
            ? new Owner(UUID.randomUUID(), owner.problemId())
            : rule.equals("PROBLEM") ? new Owner(owner.userId(), UUID.randomUUID()) : owner;
    repository.insert(
        new PendingSubmission(
            UUID.randomUUID(),
            target,
            command().program(),
            1,
            rule.equals("WINDOW") ? at.minusSeconds(3600) : at));
    sqlCandidate(source, rule, at, false);
    sqlCandidate(mutant, rule, at, true);
    assertThat(Files.readString(file)).isEqualTo(source);
    System.out.println("MUTATION CAUGHT ADMISSION_SQL_" + rule + "; original GREEN; mutant RED");
  }

  private void sqlCandidate(String source, String rule, Instant at, boolean mutant)
      throws Exception {
    Path folder = Files.createDirectories(temporary.resolve(mutant ? "mutant" : "original"));
    compileSql(source, folder);
    String prefix = "com.algoprep.judge.infrastructure.persistence";
    try (var loader =
        new URLClassLoader(new URL[] {folder.toUri().toURL()}, getClass().getClassLoader())) {
      var constructor =
          loader
              .loadClass(prefix + ".mutationcopy.JdbcSubmissionAdmissionRepository")
              .getDeclaredConstructor(ObjectProvider.class);
      constructor.setAccessible(true);
      var candidate = (SubmissionAdmissionRepository) constructor.newInstance(jdbcProvider);
      Runnable oracle =
          () -> {
            if (rule.equals("LOCK")) {
              new TransactionTemplate(manager)
                  .executeWithoutResult(
                      status -> {
                        candidate.lockPair(owner);
                        var competitor = new TransactionTemplate(manager);
                        competitor.setPropagationBehavior(
                            TransactionDefinition.PROPAGATION_REQUIRES_NEW);
                        Boolean acquired =
                            competitor.execute(
                                ignored ->
                                    jdbc.queryForObject(
                                        "SELECT pg_try_advisory_xact_lock(hashtextextended(?,0))",
                                        Boolean.class,
                                        owner.userId() + ":" + owner.problemId()));
                        assertThat(acquired).isFalse();
                      });
            } else {
              assertThat(candidate.countRecent(owner, at)).isZero();
            }
          };
      if (mutant) {
        assertThatThrownBy(oracle::run).isInstanceOf(AssertionError.class);
      } else {
        oracle.run();
      }
    }
  }

  private static void compileSql(String source, Path folder) throws Exception {
    Path file = folder.resolve("JdbcSubmissionAdmissionRepository.java");
    String prefix = "com.algoprep.judge.infrastructure.persistence";
    Files.writeString(
        file, source.replace("package " + prefix + ";", "package " + prefix + ".mutationcopy;"));
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
  }

  private static void rejected(Runnable action, String code) {
    assertThatThrownBy(action::run)
        .isInstanceOfSatisfying(
            SubmissionAdmissionException.class, e -> assertThat(e.errorCode()).isEqualTo(code));
  }
}
