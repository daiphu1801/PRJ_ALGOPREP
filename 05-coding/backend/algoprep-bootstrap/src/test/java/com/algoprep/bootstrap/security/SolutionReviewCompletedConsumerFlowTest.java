package com.algoprep.bootstrap.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
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
 * {@code SolutionReviewCompleted} consumer (DD api F1 section 2, feed {@code
 * SOLUTION_REVIEW_COMPLETED}) against real RabbitMQ and PostgreSQL. The envelope is built by hand
 * in the shape the ai-review relay publishes (DD api F5 section 33).
 */
@SpringBootTest(
    properties = {"spring.autoconfigure.exclude=", "spring.datasource.hikari.maximum-pool-size=4"})
@ActiveProfiles("test")
@Testcontainers
class SolutionReviewCompletedConsumerFlowTest {
  private static final String EXCHANGE = "algoprep.domain-events";
  private static final String KEY = "ai.solution-review.completed";
  private static final Instant HAPPENED = Instant.parse("2026-11-10T08:00:00Z");

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
          .withEnv("RABBITMQ_DEFAULT_USER", "test-srcompleted-user")
          .withEnv("RABBITMQ_DEFAULT_PASS", "test-srcompleted-password")
          .withExposedPorts(5672)
          .waitingFor(Wait.forLogMessage(".*Server startup complete.*", 1));

  @DynamicPropertySource
  static void infrastructure(DynamicPropertyRegistry properties) {
    properties.add("spring.data.redis.host", REDIS::getHost);
    properties.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
    properties.add("spring.rabbitmq.host", RABBIT::getHost);
    properties.add("spring.rabbitmq.port", () -> RABBIT.getMappedPort(5672));
    properties.add("spring.rabbitmq.username", () -> "test-srcompleted-user");
    properties.add("spring.rabbitmq.password", () -> "test-srcompleted-password");
  }

  @Autowired JdbcTemplate jdbc;
  @Autowired RabbitTemplate rabbit;

  private UUID learner;
  private UUID reviewId;

  @BeforeEach
  void fixture() {
    learner = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO identity.users(id,email,display_name,role_id,status)"
            + " SELECT ?,?,'Feed learner',id,'ACTIVE' FROM identity.roles WHERE code='STUDENT'",
        learner,
        learner + "@example.test");
    reviewId = UUID.randomUUID();
  }

  private String envelope(UUID eventId) {
    return "{\"eventId\":\""
        + eventId
        + "\",\"eventType\":\"SolutionReviewCompleted\",\"occurredAt\":\""
        + HAPPENED
        + "\",\"schemaVersion\":1,\"payload\":{\"reviewId\":\""
        + reviewId
        + "\",\"submissionId\":\""
        + UUID.randomUUID()
        + "\",\"userId\":\""
        + learner
        + "\",\"problemId\":\""
        + UUID.randomUUID()
        + "\",\"readabilityScore\":4}}";
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

  private List<Map<String, Object>> feed() {
    return jdbc.queryForList(
        "SELECT actor_user_id, scope_class_id, activity_type, ref_type, ref_id, occurred_at"
            + " FROM identity.identity_recent_activity WHERE actor_user_id = ?",
        learner);
  }

  @Test
  void theEntryIsPersonalAndARedeliveryOfTheSameEventIdCountsOnce() {
    UUID eventId = UUID.randomUUID();

    publish(envelope(eventId));
    awaitProcessed(eventId);
    publish(envelope(eventId));
    UUID marker = UUID.randomUUID();
    publish(envelope(marker));
    awaitProcessed(marker);

    List<Map<String, Object>> entries = feed();
    // One row for the event delivered twice plus one for the marker event.
    assertThat(entries).hasSize(2);
    Map<String, Object> entry = entries.getFirst();
    assertThat(entry.get("actor_user_id")).isEqualTo(learner);
    assertThat(entry.get("scope_class_id")).isNull();
    assertThat(entry.get("activity_type")).isEqualTo("SOLUTION_REVIEW_COMPLETED");
    assertThat(entry.get("ref_type")).isEqualTo("SOLUTION_REVIEW");
    assertThat(entry.get("ref_id")).isEqualTo(reviewId);
    assertThat(((Timestamp) entry.get("occurred_at")).toInstant()).isEqualTo(HAPPENED);
    assertThat(processed(eventId)).isEqualTo(1);
  }

  @Test
  void aMalformedMessageIsDroppedAndTheConsumerKeepsWorking() {
    publish("{\"eventId\":\"not-a-uuid\"}");
    UUID after = UUID.randomUUID();
    publish(envelope(after));

    awaitProcessed(after);

    assertThat(feed()).hasSize(1);
  }
}
