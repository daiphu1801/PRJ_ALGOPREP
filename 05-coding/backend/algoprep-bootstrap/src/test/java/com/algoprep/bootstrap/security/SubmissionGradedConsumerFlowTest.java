package com.algoprep.bootstrap.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
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
 * {@code SubmissionGraded} consumer (DD api F1 section 2, AC-39) against real RabbitMQ and
 * PostgreSQL: the message goes through the exchange and routing key of DD api F4 section 25.1, the
 * read models are checked in the database. The judge relay does not exist yet, so the envelope is
 * built by hand in the shape of DD api F4 section 25.2.
 */
// An empty exclude list replaces the one of the test profile (which switches off the database and
// the broker), exactly as the other flow tests do by naming only the auto-configuration they drop.
@SpringBootTest(
    properties = {"spring.autoconfigure.exclude=", "spring.datasource.hikari.maximum-pool-size=4"})
@ActiveProfiles("test")
@Testcontainers
class SubmissionGradedConsumerFlowTest {
  private static final String EXCHANGE = "algoprep.domain-events";
  private static final String KEY = "judge.submission.graded";

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
          .withEnv("RABBITMQ_DEFAULT_USER", "test-graded-user")
          .withEnv("RABBITMQ_DEFAULT_PASS", "test-graded-password")
          .withExposedPorts(5672)
          .waitingFor(Wait.forLogMessage(".*Server startup complete.*", 1));

  @DynamicPropertySource
  static void infrastructure(DynamicPropertyRegistry properties) {
    properties.add("spring.data.redis.host", REDIS::getHost);
    properties.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
    properties.add("spring.rabbitmq.host", RABBIT::getHost);
    properties.add("spring.rabbitmq.port", () -> RABBIT.getMappedPort(5672));
    properties.add("spring.rabbitmq.username", () -> "test-graded-user");
    properties.add("spring.rabbitmq.password", () -> "test-graded-password");
  }

  private static final String AT_1659 = "2026-11-03T16:59:00Z";
  private static final String AT_1701 = "2026-11-03T17:01:00Z";
  private static final String AT_1702 = "2026-11-03T17:02:00Z";
  private static final String AT_0500 = "2026-11-03T05:00:00Z";
  private static final String AT_0501 = "2026-11-03T05:01:00Z";
  private static final String AT_0502 = "2026-11-03T05:02:00Z";
  @Autowired JdbcTemplate jdbc;
  @Autowired RabbitTemplate rabbit;

  private record Graded(
      UUID eventId,
      UUID user,
      UUID problem,
      String verdict,
      String ratio,
      String language,
      String submittedAt,
      boolean first) {
    String json() {
      return "{\"eventId\":\""
          + eventId
          + "\",\"eventType\":\"SubmissionGraded\",\"occurredAt\":\"2026-11-04T00:00:00Z\","
          + "\"schemaVersion\":1,\"payload\":{\"submissionId\":\""
          + UUID.randomUUID()
          + "\",\"userId\":\""
          + user
          + "\",\"problemId\":\""
          + problem
          + "\",\"language\":\""
          + language
          + "\",\"verdict\":\""
          + verdict
          + "\",\"passedCount\":null,\"totalCount\":null,\"passedTestcaseRatio\":"
          + ratio
          + ",\"submittedAt\":\""
          + submittedAt
          + "\",\"firstAttemptForProblem\":"
          + first
          + "}}";
    }
  }

  private void publish(Graded event) {
    publish(event.json());
  }

  private void publish(String body) {
    Message message =
        MessageBuilder.withBody(body.getBytes(StandardCharsets.UTF_8))
            .setContentType("application/json")
            .build();
    rabbit.send(EXCHANGE, KEY, message);
  }

  private int processed(UUID eventId) {
    return jdbc.queryForObject(
        "SELECT count(*) FROM identity.processed_domain_events WHERE event_id = ?",
        Integer.class,
        eventId);
  }

  /** Polls for up to 15 seconds; the consumer runs on the listener container's own thread. */
  private void awaitProcessed(UUID eventId) {
    long deadline = System.nanoTime() + 15_000_000_000L;
    while (processed(eventId) != 1) {
      if (System.nanoTime() > deadline) {
        throw new AssertionError("event " + eventId + " was not consumed in time");
      }
      try {
        Thread.sleep(100);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new AssertionError("interrupted while waiting for " + eventId, e);
      }
    }
  }

  @Test
  void threeEventsFillTheReadModelsAndARedeliveryCountsNothing() {
    UUID user = UUID.randomUUID();
    UUID problemA = UUID.randomUUID();
    UUID problemB = UUID.randomUUID();
    UUID first = UUID.randomUUID();
    UUID second = UUID.randomUUID();
    UUID third = UUID.randomUUID();

    publish(new Graded(first, user, problemA, "ACCEPTED", "1", "PYTHON", AT_1659, true));
    publish(new Graded(second, user, problemA, "WRONG_ANSWER", "0.5", "JAVA", AT_1701, false));
    publish(new Graded(third, user, problemB, "WRONG_ANSWER", "0.25", "PYTHON", AT_1702, true));
    awaitProcessed(first);
    awaitProcessed(second);
    awaitProcessed(third);
    // The same event again, then a fourth one as a marker that the queue went past it.
    publish(new Graded(first, user, problemA, "ACCEPTED", "1", "PYTHON", AT_1659, true));
    UUID marker = UUID.randomUUID();
    publish(new Graded(marker, UUID.randomUUID(), problemA, "ACCEPTED", "1", "CPP", AT_0500, true));
    awaitProcessed(marker);

    assertTotalsOfThreeEvents(user);
    assertDaysOfThreeEvents(user);
    assertBestScoreOfFirstProblem(user, problemA);
  }

  private void assertTotalsOfThreeEvents(UUID user) {
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
  }

  private void assertDaysOfThreeEvents(UUID user) {
    // 16:59 UTC is the 3rd at UTC+7, 17:01 and 17:02 UTC are already the 4th.
    List<Map<String, Object>> days =
        jdbc.queryForList(
            "SELECT day::text AS day, submission_count FROM identity.user_daily_submission_stats"
                + " WHERE user_id = ? ORDER BY day",
            user);
    assertThat(days).extracting(d -> d.get("day")).containsExactly("2026-11-03", "2026-11-04");
    assertThat(days).extracting(d -> d.get("submission_count")).containsExactly(1, 2);
  }

  private void assertBestScoreOfFirstProblem(UUID user, UUID problemA) {
    BigDecimal bestA =
        jdbc.queryForObject(
            "SELECT best_ratio FROM identity.user_problem_best_score WHERE user_id = ? AND problem_id = ?",
            BigDecimal.class,
            user,
            problemA);
    assertThat(bestA).isEqualByComparingTo("1");
    assertThat(
            jdbc.queryForObject(
                "SELECT best_verdict FROM identity.user_problem_best_score WHERE user_id = ? AND problem_id = ?",
                String.class,
                user,
                problemA))
        .isEqualTo("ACCEPTED");
  }

  @Test
  void aWorseAttemptKeepsTheBestScoreAndASystemErrorIsOnlyCounted() {
    UUID user = UUID.randomUUID();
    UUID problem = UUID.randomUUID();
    UUID good = UUID.randomUUID();
    UUID worse = UUID.randomUUID();
    UUID broken = UUID.randomUUID();

    publish(new Graded(good, user, problem, "WRONG_ANSWER", "0.75", "JAVA", AT_0500, true));
    publish(new Graded(worse, user, problem, "COMPILE_ERROR", "null", "JAVA", AT_0501, false));
    publish(new Graded(broken, user, problem, "SYSTEM_ERROR", "null", "JAVA", AT_0502, false));
    awaitProcessed(broken);
    awaitProcessed(worse);

    assertThat(
            jdbc.queryForObject(
                "SELECT best_ratio FROM identity.user_problem_best_score WHERE user_id = ?",
                BigDecimal.class,
                user))
        .isEqualByComparingTo("0.75");
    assertThat(
            jdbc.queryForObject(
                "SELECT best_verdict FROM identity.user_problem_best_score WHERE user_id = ?",
                String.class,
                user))
        .isEqualTo("WRONG_ANSWER");
    Map<String, Object> stats =
        jdbc.queryForMap(
            "SELECT total_submissions, ac_eligible_count, accepted_count"
                + " FROM identity.user_submission_stats WHERE user_id = ?",
            user);
    assertThat(stats.get("total_submissions")).isEqualTo(3);
    assertThat(stats.get("ac_eligible_count")).isEqualTo(2);
    assertThat(stats.get("accepted_count")).isEqualTo(0);
  }

  @Test
  void aMalformedMessageIsDroppedAndTheConsumerKeepsWorking() {
    publish("{\"eventId\":\"not-a-uuid\"}");
    UUID user = UUID.randomUUID();
    UUID after = UUID.randomUUID();
    publish(new Graded(after, user, UUID.randomUUID(), "ACCEPTED", "1", "CPP", AT_0500, true));

    awaitProcessed(after);

    assertThat(
            jdbc.queryForObject(
                "SELECT total_submissions FROM identity.user_submission_stats WHERE user_id = ?",
                Integer.class,
                user))
        .isEqualTo(1);
  }
}
