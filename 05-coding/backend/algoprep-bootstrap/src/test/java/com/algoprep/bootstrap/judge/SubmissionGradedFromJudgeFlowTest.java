package com.algoprep.bootstrap.judge;

import static org.assertj.core.api.Assertions.assertThat;

import com.algoprep.judge.application.ports.in.BeginSubmissionCompilationUseCase;
import com.algoprep.judge.application.ports.in.BeginSubmissionJudgingUseCase;
import com.algoprep.judge.application.ports.in.FinalizeSubmissionUseCase;
import com.algoprep.judge.application.ports.in.IssueSubmissionRunTokenUseCase;
import com.algoprep.judge.application.ports.in.RecordSubmissionTestcaseUseCase;
import com.algoprep.judge.application.ports.in.RelayJudgeOutboxUseCase;
import com.algoprep.judge.domain.model.WorkerRecords.FinishCommand;
import com.algoprep.judge.domain.model.WorkerRecords.FinishKind;
import com.algoprep.judge.domain.model.WorkerRecords.TestcaseWrite;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.MountableFile;

/**
 * AC-39 seen from the producer: the Outbox rows come from the real F4 grading lifecycle (compile,
 * judge, record, finalize), travel through the real relay and RabbitMQ, and the real F1 consumer
 * fills the read models. Only the submission rows entering the queue are inserted by hand.
 */
@SpringBootTest(
    properties = {
      "spring.autoconfigure.exclude=",
      "judge.outbox.enabled=false",
      "spring.datasource.hikari.maximum-pool-size=6"
    })
@ActiveProfiles("test")
@Testcontainers
class SubmissionGradedFromJudgeFlowTest {
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
  static final GenericContainer<?> RABBIT =
      new GenericContainer<>("rabbitmq:4.2.9-management-alpine")
          .withEnv("RABBITMQ_DEFAULT_USER", "test-judge-graded-user")
          .withEnv("RABBITMQ_DEFAULT_PASS", "test-judge-graded-password")
          .withExposedPorts(5672)
          .waitingFor(Wait.forLogMessage(".*Server startup complete.*", 1));

  @DynamicPropertySource
  static void infrastructure(DynamicPropertyRegistry properties) {
    properties.add("spring.data.redis.host", REDIS::getHost);
    properties.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
    properties.add("spring.rabbitmq.host", RABBIT::getHost);
    properties.add("spring.rabbitmq.port", () -> RABBIT.getMappedPort(5672));
    properties.add("spring.rabbitmq.username", () -> "test-judge-graded-user");
    properties.add("spring.rabbitmq.password", () -> "test-judge-graded-password");
  }

  @Autowired JdbcTemplate jdbc;
  @Autowired RelayJudgeOutboxUseCase relay;
  @Autowired BeginSubmissionCompilationUseCase compile;
  @Autowired BeginSubmissionJudgingUseCase judging;
  @Autowired IssueSubmissionRunTokenUseCase tokens;
  @Autowired RecordSubmissionTestcaseUseCase results;
  @Autowired FinalizeSubmissionUseCase finalizer;

  @BeforeEach
  void clean() {
    jdbc.update("DELETE FROM judge.outbox_events");
  }

  @Test
  void gradedSubmissionsFromTheRealJudgeFillTheF1ReadModelsAndARedeliveryCountsNothing()
      throws Exception {
    UUID user = student();
    UUID problem = UUID.randomUUID();
    UUID first = grade(user, problem, "PYTHON", "AC");
    UUID second = grade(user, problem, "JAVA", "WA");
    UUID third = grade(user, problem, "PYTHON", "WA");

    // Three SubmissionGraded and one SubmissionAccepted, all written by the finalize step.
    assertThat(relay.relay()).isEqualTo(4);
    for (UUID submission : List.of(first, second, third)) {
      awaitProcessed(gradedEvent(submission));
    }

    Map<String, Object> stats =
        jdbc.queryForMap(
            "SELECT total_submissions, ac_eligible_count, accepted_count, first_try_accepted_count,"
                + " top_language, language_counts::text AS counts"
                + " FROM identity.user_submission_stats WHERE user_id = ?",
            user);
    assertThat(stats.get("total_submissions")).isEqualTo(3);
    assertThat(stats.get("ac_eligible_count")).isEqualTo(3);
    assertThat(stats.get("accepted_count")).isEqualTo(1);
    assertThat(stats.get("first_try_accepted_count")).isEqualTo(1);
    assertThat(stats.get("top_language")).isEqualTo("PYTHON");
    assertThat((String) stats.get("counts")).contains("\"JAVA\": 1").contains("\"PYTHON\": 2");
    assertThat(daysInF1(user)).isEqualTo(daysInJudge(user));

    // The broker confirmed the first message but the commit was lost: it is published again.
    jdbc.update("UPDATE judge.outbox_events SET published_at=NULL WHERE id=?", gradedEvent(first));
    assertThat(relay.relay()).isEqualTo(1);
    UUID marker = grade(student(), UUID.randomUUID(), "CPP", "WA");
    assertThat(relay.relay()).isEqualTo(1);
    awaitProcessed(gradedEvent(marker));

    assertThat(
            jdbc.queryForObject(
                "SELECT total_submissions FROM identity.user_submission_stats WHERE user_id = ?",
                Integer.class,
                user))
        .isEqualTo(3);
    assertThat(daysInF1(user)).isEqualTo(daysInJudge(user));
  }

  private UUID student() {
    UUID user = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO identity.users(id,email,display_name,role_id,status) SELECT ?,?,'Judge fixture',id,'ACTIVE' FROM identity.roles WHERE code='STUDENT'",
        user,
        user + "@example.test");
    return user;
  }

  /** Runs one single-testcase submission through the real worker lifecycle and returns its id. */
  private UUID grade(UUID user, UUID problem, String language, String verdict) {
    UUID id = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO judge.submissions(id,user_id,problem_id,language,submission_model,source_code,source_input_method,status,testcase_set_version_at_grading) VALUES(?,?,?,?,'STANDARD_IO','x','EDITOR','PENDING',7)",
        id,
        user,
        problem,
        language);
    assertThat(compile.begin(id, 9)).isPresent();
    assertThat(judging.beginJudging(id, 1)).isPresent();
    UUID testcase = UUID.randomUUID();
    var token = tokens.issue(id, testcase).orElseThrow();
    assertThat(
            results.record(
                new TestcaseWrite(
                    id, testcase, token.id(), 1, true, verdict, 2, 3, "out", null, false)))
        .isTrue();
    assertThat(finalizer.finish(new FinishCommand(id, FinishKind.COMPLETE, null, null)))
        .isPresent();
    return id;
  }

  private UUID gradedEvent(UUID submission) {
    return jdbc.queryForObject(
        "SELECT id FROM judge.outbox_events WHERE aggregate_id=? AND event_type='SubmissionGraded'",
        UUID.class,
        submission);
  }

  /** Daily counts as F1 stored them, keyed by the UTC+7 day. */
  private Map<String, Integer> daysInF1(UUID user) {
    return jdbc
        .queryForList(
            "SELECT day::text AS day, submission_count FROM identity.user_daily_submission_stats WHERE user_id = ?",
            user)
        .stream()
        .collect(
            Collectors.toMap(
                row -> (String) row.get("day"), row -> (Integer) row.get("submission_count")));
  }

  /** The same counts computed straight from the judge's submissions, the oracle for F1's days. */
  private Map<String, Integer> daysInJudge(UUID user) {
    return jdbc
        .queryForList(
            "SELECT (submitted_at AT TIME ZONE 'Asia/Ho_Chi_Minh')::date::text AS day, count(*)::int AS n FROM judge.submissions WHERE user_id = ? GROUP BY 1",
            user)
        .stream()
        .collect(Collectors.toMap(row -> (String) row.get("day"), row -> (Integer) row.get("n")));
  }

  private void awaitProcessed(UUID eventId) throws Exception {
    long deadline = System.nanoTime() + Duration.ofSeconds(15).toNanos();
    while (jdbc.queryForObject(
            "SELECT count(*) FROM identity.processed_domain_events WHERE event_id=?",
            Integer.class,
            eventId)
        == 0) {
      assertThat(System.nanoTime())
          .as("event " + eventId + " consumed in time")
          .isLessThan(deadline);
      Thread.sleep(50);
    }
  }
}
