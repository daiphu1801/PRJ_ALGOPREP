package com.algoprep.bootstrap.judge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.algoprep.judge.application.dto.SubmissionProgress;
import com.algoprep.judge.application.ports.in.BeginSubmissionCompilationUseCase;
import com.algoprep.judge.application.ports.in.BeginSubmissionJudgingUseCase;
import com.algoprep.judge.application.ports.in.FinalizeSubmissionUseCase;
import com.algoprep.judge.application.ports.in.IssueSubmissionRunTokenUseCase;
import com.algoprep.judge.application.ports.in.RecordSubmissionTestcaseUseCase;
import com.algoprep.judge.application.ports.out.SubmissionProgressPort;
import com.algoprep.judge.domain.exception.WorkerPersistenceException;
import com.algoprep.judge.domain.model.WorkerRecords.FinishCommand;
import com.algoprep.judge.domain.model.WorkerRecords.FinishKind;
import com.algoprep.judge.domain.model.WorkerRecords.Outcome;
import com.algoprep.judge.domain.model.WorkerRecords.TestcaseWrite;
import com.algoprep.judge.domain.ports.out.SubmissionWorkerRepository;
import java.io.ByteArrayOutputStream;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import javax.tools.ToolProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.MountableFile;
import tools.jackson.databind.json.JsonMapper;

/**
 * PostgreSQL worker writes only: no live consumer or execution engine is installed by this slice.
 */
@SpringBootTest(
    properties = {
      "spring.autoconfigure.exclude=org.springframework.boot.data.redis.autoconfigure.RedisAutoConfiguration,org.springframework.boot.amqp.autoconfigure.RabbitAutoConfiguration",
      "spring.datasource.hikari.maximum-pool-size=6"
    })
@ActiveProfiles("test")
@Testcontainers
@Import(WorkerPersistenceFlowTest.Recording.class)
class WorkerPersistenceFlowTest {
  @Container @ServiceConnection
  static final PostgreSQLContainer POSTGRES =
      new PostgreSQLContainer("postgres:16.15-alpine")
          .withDatabaseName("algoprep")
          .withCopyFileToContainer(
              MountableFile.forHostPath("../../database/init/01-schemas.sql"),
              "/docker-entrypoint-initdb.d/01-schemas.sql");

  @Autowired JdbcTemplate jdbc;
  @Autowired BeginSubmissionCompilationUseCase compile;
  @Autowired BeginSubmissionJudgingUseCase judging;
  @Autowired IssueSubmissionRunTokenUseCase tokens;
  @Autowired RecordSubmissionTestcaseUseCase results;
  @Autowired FinalizeSubmissionUseCase finalizer;
  @Autowired PlatformTransactionManager manager;
  @Autowired ObjectProvider<JdbcClient> jdbcProvider;
  @TempDir Path temporary;
  private final JsonMapper json = JsonMapper.builder().findAndAddModules().build();

  @TestConfiguration
  static class Recording {
    static final List<SubmissionProgress> EVENTS = new CopyOnWriteArrayList<>();
    static final List<UUID> OWNERS = new CopyOnWriteArrayList<>();

    @Bean
    @Primary
    SubmissionProgressPort recordProgress() {
      return (owner, progress) -> {
        assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isTrue();
        TransactionSynchronizationManager.registerSynchronization(
            new TransactionSynchronization() {
              @Override
              public void afterCommit() {
                EVENTS.add(progress);
                OWNERS.add(owner);
              }
            });
      };
    }
  }

  @BeforeEach
  void clearRecording() {
    Recording.EVENTS.clear();
    Recording.OWNERS.clear();
  }

  private UUID pending(String model) {
    UUID id = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO judge.submissions(id,user_id,problem_id,language,submission_model,source_code,source_input_method,status,testcase_set_version_at_grading) VALUES(?,?,?,'PYTHON',?,'private source','EDITOR','PENDING',7)",
        id,
        UUID.randomUUID(),
        UUID.randomUUID(),
        model);
    return id;
  }

  private UUID start(String model, int total) {
    UUID id = pending(model);
    assertThat(compile.begin(id, 9)).isPresent();
    assertThat(judging.beginJudging(id, total)).isPresent();
    return id;
  }

  private TestcaseWrite write(UUID id, int order, boolean sample, String verdict) {
    UUID testcase = UUID.randomUUID();
    var token = tokens.issue(id, testcase).orElseThrow();
    return new TestcaseWrite(
        id,
        testcase,
        token.id(),
        order,
        sample,
        verdict,
        order * 2,
        order * 3,
        "private-output".repeat(500),
        "private-stderr".repeat(500),
        true);
  }

  private FinishCommand complete(UUID id) {
    return new FinishCommand(id, FinishKind.COMPLETE, null, null);
  }

  private FinishCommand fault(UUID id) {
    return new FinishCommand(id, FinishKind.SYSTEM_ERROR, "WORKER_TIMEOUT", null);
  }

  private long rows(UUID id) {
    return jdbc.queryForObject(
        "SELECT count(*) FROM judge.submission_testcase_results WHERE submission_id=?",
        Long.class,
        id);
  }

  private long events(UUID id, String type) {
    return jdbc.queryForObject(
        "SELECT count(*) FROM judge.outbox_events WHERE aggregate_id=? AND event_type=?",
        Long.class,
        id,
        type);
  }

  private String status(UUID id) {
    return jdbc.queryForObject("SELECT status FROM judge.submissions WHERE id=?", String.class, id);
  }

  @Test
  void completeLifecyclePinsVersionAndPersistsAllCasesWithFirstFailureAndIndependentScore() {
    UUID id = start("STANDARD_IO", 3);
    assertThat(results.record(write(id, 1, true, "AC"))).isTrue();
    assertThat(results.record(write(id, 2, false, "WA"))).isTrue();
    assertThat(results.record(write(id, 3, true, "TLE"))).isTrue();
    var finished = finalizer.finish(complete(id)).orElseThrow();
    assertThat(finished.outcome().status()).isEqualTo("WRONG_ANSWER");
    assertThat(finished.outcome().score()).isEqualByComparingTo("0.333");
    assertThat(finished.outcome().runtime()).isEqualTo(6);
    assertThat(finished.outcome().memory()).isEqualTo(9);
    var row = jdbc.queryForMap("SELECT * FROM judge.submissions WHERE id=?", id);
    assertThat(row.get("testcase_set_version_at_grading")).isEqualTo(7);
    assertThat(row.get("spec_version")).isEqualTo(9);
    assertThat(row.get("started_grading_at")).isNotNull();
    assertThat(row.get("finished_at")).isEqualTo(row.get("updated_at"));
    assertThat(rows(id)).isEqualTo(3);
    assertThat(events(id, "SubmissionGraded")).isEqualTo(1);
    assertThat(events(id, "SubmissionAccepted")).isZero();
    assertThat(Recording.EVENTS.stream().map(SubmissionProgress::seq).toList())
        .containsExactly(1, 2, 3, 4, 5, 6);
    assertThat(Recording.OWNERS).containsOnly(finished.state().owner().userId());
    assertThat(Recording.EVENTS.getLast().isFinal()).isTrue();
  }

  @ParameterizedTest
  @ValueSource(strings = {"TLE", "MLE", "RE"})
  void individualFailureMapsToItsDocumentedTerminalStatus(String verdict) {
    UUID id = start("STANDARD_IO", 1);
    results.record(write(id, 1, false, verdict));
    var finished = finalizer.finish(complete(id)).orElseThrow();
    assertThat(finished.state().status())
        .isEqualTo(verdict.equals("RE") ? "RUNTIME_ERROR" : "TIME_LIMIT_EXCEEDED");
    assertThat(finished.outcome().score()).isEqualByComparingTo("0.000");
  }

  @Test
  void acceptedAppendsTwoExactPersistedEnvelopesWithTheOriginalSubmissionTime() {
    UUID id = start("STANDARD_IO", 1);
    results.record(write(id, 1, true, "AC"));
    var finished = finalizer.finish(complete(id)).orElseThrow();
    assertThat(events(id, "SubmissionGraded")).isEqualTo(1);
    assertThat(events(id, "SubmissionAccepted")).isEqualTo(1);
    var stored =
        jdbc.queryForList(
            "SELECT id,event_type,payload::text AS payload,published_at,publish_attempts FROM judge.outbox_events WHERE aggregate_id=?",
            id);
    for (var row : stored) {
      var envelope = json.readTree((String) row.get("payload"));
      assertThat(envelope.propertyNames())
          .containsExactlyInAnyOrder(
              "eventId", "eventType", "occurredAt", "schemaVersion", "payload");
      assertThat(envelope.path("eventId").asString()).isEqualTo(row.get("id").toString());
      assertThat(envelope.path("schemaVersion").asInt()).isEqualTo(1);
      var payload = envelope.path("payload");
      assertThat(payload.path("submissionId").asString()).isEqualTo(id.toString());
      if (row.get("event_type").equals("SubmissionGraded")) {
        assertThat(payload.size()).isEqualTo(10);
        assertThat(payload.path("submittedAt").asString())
            .isEqualTo(finished.state().submittedAt().toString());
        assertThat(payload.path("passedTestcaseRatio").decimalValue())
            .isEqualByComparingTo("1.000");
        assertThat(payload.path("firstAttemptForProblem").asBoolean()).isTrue();
      } else {
        assertThat(payload.propertyNames())
            .containsExactlyInAnyOrder("submissionId", "userId", "problemId", "language");
      }
      assertThat(row.get("payload").toString())
          .doesNotContain("private", "source", "stderr", "failureReason", "classId");
      assertThat(row.get("published_at")).isNull();
      assertThat(row.get("publish_attempts")).isEqualTo(0);
    }
  }

  @ParameterizedTest
  @CsvSource({
    "STANDARD_IO,true,WA,true",
    "STANDARD_IO,true,AC,false",
    "STANDARD_IO,false,WA,false",
    "FUNCTION_WRAPPER,true,WA,false"
  })
  void snippetPrivacyIsEnforcedAtPersistence(
      String model, boolean sample, String verdict, boolean stderrAllowed) {
    UUID id = start(model, 1);
    var write = write(id, 1, sample, verdict);
    results.record(write);
    var row =
        jdbc.queryForMap(
            "SELECT actual_output_snippet,stderr_snippet FROM judge.submission_testcase_results WHERE submission_id=?",
            id);
    if (sample) {
      assertThat((String) row.get("actual_output_snippet")).hasSize(4096);
    } else {
      assertThat(row.get("actual_output_snippet")).isNull();
    }
    if (stderrAllowed) {
      assertThat((String) row.get("stderr_snippet")).hasSize(4096);
    } else {
      assertThat(row.get("stderr_snippet")).isNull();
    }
  }

  @Test
  void duplicateDeliveryCannotConsumeAnotherTokenOrPushAnotherResult() {
    UUID id = start("STANDARD_IO", 1);
    var write = write(id, 1, false, "WA");
    assertThat(results.record(write)).isTrue();
    int pushes = Recording.EVENTS.size();
    assertThat(results.record(write)).isFalse();
    assertThat(tokens.issue(id, write.testcaseId())).isEmpty();
    assertThat(rows(id)).isEqualTo(1);
    assertThat(Recording.EVENTS).hasSize(pushes);
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM judge.judge_run_tokens WHERE submission_id=? AND consumed_at IS NOT NULL",
                Long.class,
                id))
        .isEqualTo(1);
  }

  @Test
  void terminalAndMissingJobsIgnoreLateWritesAndFinalization() {
    UUID id = start("STANDARD_IO", 1);
    var write = write(id, 1, false, "AC");
    finalizer.finish(fault(id));
    int pushes = Recording.EVENTS.size();
    assertThat(results.record(write)).isFalse();
    assertThat(compile.begin(id, 10)).isEmpty();
    assertThat(judging.beginJudging(id, 2)).isEmpty();
    assertThat(tokens.issue(id, write.testcaseId())).isEmpty();
    assertThat(finalizer.finish(complete(id))).isEmpty();
    assertThat(finalizer.finish(fault(UUID.randomUUID()))).isEmpty();
    assertThat(rows(id)).isZero();
    assertThat(events(id, "SubmissionGraded")).isEqualTo(1);
    assertThat(Recording.EVENTS).hasSize(pushes);
    assertThat(
            jdbc.queryForObject(
                "SELECT consumed_at FROM judge.judge_run_tokens WHERE id=?",
                java.sql.Timestamp.class,
                write.tokenId()))
        .isNull();
  }

  @Test
  void retriesWithoutResultsKeepStartTimeAndPinnedVersionWhilePartialResultsCannotRestart() {
    UUID id = start("STANDARD_IO", 2);
    var started =
        jdbc.queryForObject(
            "SELECT started_grading_at FROM judge.submissions WHERE id=?",
            java.sql.Timestamp.class,
            id);
    assertThat(compile.begin(id, 10)).isPresent();
    assertThat(judging.beginJudging(id, 2)).isPresent();
    assertThat(
            jdbc.queryForObject(
                "SELECT started_grading_at FROM judge.submissions WHERE id=?",
                java.sql.Timestamp.class,
                id))
        .isEqualTo(started);
    results.record(write(id, 1, false, "AC"));
    assertThat(compile.begin(id, 11)).isEmpty();
    finalizer.finish(fault(id));
    assertThat(rows(id)).isEqualTo(1);
    assertThat(
            jdbc.queryForObject(
                "SELECT testcase_set_version_at_grading FROM judge.submissions WHERE id=?",
                Integer.class,
                id))
        .isEqualTo(7);
  }

  @Test
  void finalizationWinnerIsUniqueAcrossConcurrentWorkers() throws Exception {
    UUID id = start("STANDARD_IO", 1);
    results.record(write(id, 1, false, "AC"));
    try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
      Callable<Boolean> accepted = () -> finalizer.finish(complete(id)).isPresent();
      Callable<Boolean> timeout = () -> finalizer.finish(fault(id)).isPresent();
      var futures = executor.invokeAll(List.of(accepted, timeout));
      assertThat(List.of(futures.get(0).get(), futures.get(1).get()))
          .containsExactlyInAnyOrder(true, false);
    }
    assertThat(events(id, "SubmissionGraded")).isEqualTo(1);
    assertThat(status(id)).isIn("ACCEPTED", "SYSTEM_ERROR");
    assertThat(events(id, "SubmissionAccepted")).isEqualTo(status(id).equals("ACCEPTED") ? 1 : 0);
  }

  @Test
  void mappedCompileErrorAndSystemErrorHaveNullPublicScoreAndStableFinalSequence() {
    UUID compileId = pending("STANDARD_IO");
    compile.begin(compileId, 2);
    finalizer.finish(
        new FinishCommand(compileId, FinishKind.COMPILE_ERROR, null, "mapped".repeat(3000)));
    assertThat(
            jdbc.queryForObject(
                "SELECT length(compile_error_message) FROM judge.submissions WHERE id=?",
                Integer.class,
                compileId))
        .isEqualTo(10000);
    assertThat(Recording.EVENTS.getLast().seq()).isEqualTo(3);
    UUID systemId = start("STANDARD_IO", 3);
    results.record(write(systemId, 1, false, "WA"));
    finalizer.finish(fault(systemId));
    assertThat(Recording.EVENTS.getLast().seq()).isEqualTo(6);
    var event = json.readTree(json.writeValueAsString(Recording.EVENTS.getLast()));
    assertThat(event.path("scoreRatio").isNull()).isTrue();
    assertThat(event.path("passedCount").isNull()).isTrue();
    assertThat(event.path("totalCount").isNull()).isTrue();
    assertThat(rows(systemId)).isEqualTo(1);
  }

  @Test
  void wrongTokenAndIncompleteFinalizationRollBackWithoutPush() {
    UUID id = start("STANDARD_IO", 2);
    var write = write(id, 2, false, "AC");
    int pushes = Recording.EVENTS.size();
    assertThatThrownBy(() -> results.record(write))
        .isInstanceOf(WorkerPersistenceException.class)
        .hasNoCause();
    var wrong =
        new TestcaseWrite(
            id, write.testcaseId(), UUID.randomUUID(), 1, false, "AC", 1, 1, null, null, false);
    assertThatThrownBy(() -> results.record(wrong))
        .isInstanceOf(WorkerPersistenceException.class)
        .hasNoCause();
    assertThatThrownBy(() -> finalizer.finish(complete(id)))
        .isInstanceOf(WorkerPersistenceException.class)
        .hasNoCause();
    assertThat(status(id)).isEqualTo("JUDGING");
    assertThat(rows(id)).isZero();
    assertThat(events(id, "SubmissionGraded")).isZero();
    assertThat(Recording.EVENTS).hasSize(pushes);
  }

  private void deferredFailure(String table) {
    jdbc.execute(
        "CREATE FUNCTION judge.reject_worker_write() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN RAISE EXCEPTION 'private payload'; END $$");
    jdbc.execute(
        "CREATE CONSTRAINT TRIGGER reject_worker_write AFTER INSERT ON judge."
            + table
            + " DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION judge.reject_worker_write()");
  }

  private void dropFailure(String table) {
    jdbc.execute("DROP TRIGGER reject_worker_write ON judge." + table);
    jdbc.execute("DROP FUNCTION judge.reject_worker_write()");
  }

  @ParameterizedTest
  @ValueSource(strings = {"outbox_events", "submission_testcase_results"})
  void commitFailureRollsBackStateRowsTokensOutboxAndProgress(String table) {
    UUID id = start("STANDARD_IO", 1);
    var write = write(id, 1, false, "AC");
    if (table.equals("outbox_events")) {
      results.record(write);
    }
    int pushes = Recording.EVENTS.size();
    deferredFailure(table);
    try {
      assertThatThrownBy(
              () -> {
                if (table.equals("outbox_events")) {
                  finalizer.finish(complete(id));
                } else {
                  results.record(write);
                }
              })
          .isInstanceOf(WorkerPersistenceException.class)
          .hasNoCause()
          .hasMessage("Worker persistence is unavailable");
      assertThat(status(id)).isEqualTo("JUDGING");
      assertThat(events(id, "SubmissionGraded")).isZero();
      assertThat(events(id, "SubmissionAccepted")).isZero();
      assertThat(Recording.EVENTS).hasSize(pushes);
      assertThat(rows(id)).isEqualTo(table.equals("outbox_events") ? 1 : 0);
      assertThat(
              jdbc.queryForObject(
                  "SELECT consumed_at IS NOT NULL FROM judge.judge_run_tokens WHERE id=?",
                  Boolean.class,
                  write.tokenId()))
          .isEqualTo(table.equals("outbox_events"));
    } finally {
      dropFailure(table);
    }
  }

  @Test
  void systemErrorHistoryStillCountsAsTheEarlierAttempt() {
    UUID id = start("STANDARD_IO", 1);
    UUID older = pending("STANDARD_IO");
    jdbc.update(
        "UPDATE judge.submissions older SET user_id=s.user_id,problem_id=s.problem_id,submitted_at=s.submitted_at-interval '1 second',status='SYSTEM_ERROR',failure_reason='SANDBOX_FAULT',finished_at=clock_timestamp() FROM judge.submissions s WHERE older.id=? AND s.id=?",
        older,
        id);
    results.record(write(id, 1, true, "AC"));
    finalizer.finish(complete(id));
    assertThat(
            jdbc.queryForObject(
                "SELECT (payload->'payload'->>'firstAttemptForProblem')::boolean FROM judge.outbox_events WHERE aggregate_id=? AND event_type='SubmissionGraded'",
                Boolean.class,
                id))
        .isFalse();
  }

  @Test
  void snippetsPreserveWholeUnicodeCharactersAndHonorTheAdminStderrFlag() {
    UUID id = start("STANDARD_IO", 1);
    var issued = write(id, 1, true, "WA");
    String output = new String(Character.toChars(0x10400)).repeat(4097);
    var write =
        new TestcaseWrite(
            id,
            issued.testcaseId(),
            issued.tokenId(),
            1,
            true,
            "WA",
            1,
            1,
            output,
            "private",
            false);
    results.record(write);
    var row =
        jdbc.queryForMap(
            "SELECT actual_output_snippet,stderr_snippet FROM judge.submission_testcase_results WHERE submission_id=?",
            id);
    String actual = (String) row.get("actual_output_snippet");
    assertThat(actual.codePointCount(0, actual.length())).isEqualTo(4096);
    assertThat(actual).isEqualTo(output.substring(0, 8192));
    assertThat(row.get("stderr_snippet")).isNull();
  }

  @ParameterizedTest
  @ValueSource(strings = {"HEARTBEAT", "STDERR", "FINAL"})
  void independentlyCompiledSqlMutationsViolateTheOriginalOracle(String rule) throws Exception {
    Path file =
        Path.of(
            "../algoprep-judge/src/main/java/com/algoprep/judge/infrastructure/persistence/JdbcSubmissionWorkerRepository.java");
    String original = Files.readString(file);
    String changed =
        switch (rule) {
          case "HEARTBEAT" ->
              original.replace(
                  "WHERE id=:id AND status='JUDGING' RETURNING *", "WHERE id=:id RETURNING *");
          case "STDERR" -> original.replace("state.model().equals(\"STANDARD_IO\")", "true");
          default ->
              original.replace(
                  "WHERE s.id=:id AND s.status IN ('PENDING','COMPILING','JUDGING') RETURNING s.*",
                  "WHERE s.id=:id RETURNING s.*");
        };
    assertThat(changed).isNotEqualTo(original);
    UUID id = start("FUNCTION_WRAPPER", 1);
    var write = write(id, 1, true, "WA");
    if (!rule.equals("STDERR")) {
      finalizer.finish(fault(id));
    }
    assertThat(mutationOracle(copy(original, rule + "original"), rule, write)).isTrue();
    assertThat(mutationOracle(copy(changed, rule + "mutant"), rule, write)).isFalse();
    assertThat(Files.readString(file)).isEqualTo(original);
    System.out.println(
        "MUTATION CAUGHT WORKER_" + rule + "; source copy compiled; original green; mutant red");
  }

  private SubmissionWorkerRepository copy(String source, String variant) throws Exception {
    String pkg = "com.algoprep.judge.infrastructure.persistence.mutationcopy";
    Path dir = Files.createDirectories(temporary.resolve(variant));
    Path file = dir.resolve("JdbcSubmissionWorkerRepository.java");
    Files.writeString(
        file,
        source.replace(
            "package com.algoprep.judge.infrastructure.persistence;", "package " + pkg + ";"));
    var error = new ByteArrayOutputStream();
    int exit =
        ToolProvider.getSystemJavaCompiler()
            .run(
                null,
                error,
                error,
                "-proc:none",
                "-encoding",
                "UTF-8",
                "-classpath",
                System.getProperty("java.class.path"),
                "-d",
                dir.toString(),
                file.toString());
    assertThat(exit).withFailMessage(error.toString(StandardCharsets.UTF_8)).isZero();
    try (var loader =
        new URLClassLoader(new java.net.URL[] {dir.toUri().toURL()}, getClass().getClassLoader())) {
      var type = loader.loadClass(pkg + ".JdbcSubmissionWorkerRepository");
      var constructor = type.getDeclaredConstructors()[0];
      constructor.setAccessible(true);
      return (SubmissionWorkerRepository) constructor.newInstance(jdbcProvider);
    }
  }

  private boolean mutationOracle(
      SubmissionWorkerRepository copy, String rule, TestcaseWrite write) {
    return new TransactionTemplate(manager)
        .execute(
            tx -> {
              tx.setRollbackOnly();
              if (rule.equals("HEARTBEAT")) {
                return copy.heartbeat(write.submissionId()).isEmpty();
              }
              var state = copy.lock(write.submissionId()).orElseThrow();
              if (rule.equals("STDERR")) {
                copy.heartbeat(write.submissionId());
                copy.save(write, state, UUID.randomUUID());
                return jdbc.queryForObject(
                        "SELECT stderr_snippet FROM judge.submission_testcase_results WHERE submission_id=?",
                        String.class,
                        write.submissionId())
                    == null;
              }
              try {
                return copy.finish(
                        state,
                        new Outcome(
                            "COMPILE_ERROR", 0, null, null, null, null, null, "mapped", null))
                    .status()
                    .equals("SYSTEM_ERROR");
              } catch (IncorrectResultSizeDataAccessException expected) {
                return true;
              }
            });
  }
}
