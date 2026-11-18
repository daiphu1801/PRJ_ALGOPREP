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
 * {@code SubmissionAccepted} consumer (DD api F1 section 2) against real RabbitMQ and PostgreSQL.
 * The event names no class, so the test seeds enrollments in identity and assignments in
 * problem-bank and checks that the feed entry lands in the right class. The judge relay does not
 * exist yet, so the envelope is built by hand in the shape of DD api F4 sections 25.1 and 25.3.
 */
// An empty exclude list replaces the one of the test profile (which switches off the database and
// the broker), exactly as the other flow tests do by naming only the auto-configuration they drop.
@SpringBootTest(
    properties = {"spring.autoconfigure.exclude=", "spring.datasource.hikari.maximum-pool-size=4"})
@ActiveProfiles("test")
@Testcontainers
class SubmissionAcceptedConsumerFlowTest {
  private static final String EXCHANGE = "algoprep.domain-events";
  private static final String KEY = "judge.submission.accepted";
  private static final Instant HAPPENED = Instant.parse("2026-11-03T10:00:00Z");
  private static final Instant BEFORE = Instant.parse("2026-11-01T00:00:00Z");

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
          .withEnv("RABBITMQ_DEFAULT_USER", "test-accepted-user")
          .withEnv("RABBITMQ_DEFAULT_PASS", "test-accepted-password")
          .withExposedPorts(5672)
          .waitingFor(Wait.forLogMessage(".*Server startup complete.*", 1));

  @DynamicPropertySource
  static void infrastructure(DynamicPropertyRegistry properties) {
    properties.add("spring.data.redis.host", REDIS::getHost);
    properties.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
    properties.add("spring.rabbitmq.host", RABBIT::getHost);
    properties.add("spring.rabbitmq.port", () -> RABBIT.getMappedPort(5672));
    properties.add("spring.rabbitmq.username", () -> "test-accepted-user");
    properties.add("spring.rabbitmq.password", () -> "test-accepted-password");
  }

  @Autowired JdbcTemplate jdbc;
  @Autowired RabbitTemplate rabbit;

  private UUID instructor;
  private UUID problem;

  @BeforeEach
  void fixture() {
    instructor = user("INSTRUCTOR");
    problem = problem();
  }

  private UUID user(String role) {
    UUID id = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO identity.users(id,email,display_name,role_id,status)"
            + " SELECT ?,?,'Feed actor',id,'ACTIVE' FROM identity.roles WHERE code=?",
        id,
        id + "@example.test",
        role);
    return id;
  }

  private UUID problem() {
    UUID id = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO problem.problems(id,code,title,author_id,updated_by,level_id,"
            + "time_limit_ms,memory_limit_mb,max_output_size_kb,status,deleted)"
            + " VALUES (?,?,'Feed problem',?,?,'20000000-0000-4000-8000-000000000001',"
            + "1000,256,64,'PUBLISHED',false)",
        id,
        id.toString(),
        instructor,
        instructor);
    return id;
  }

  private UUID classroom() {
    UUID id = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO identity.classes(id,instructor_id,name) VALUES (?,?,'Feed class')",
        id,
        instructor);
    return id;
  }

  private void enroll(UUID classId, UUID student, Instant joinedAt) {
    jdbc.update(
        "INSERT INTO identity.class_enrollments(id,class_id,student_id,joined_at) VALUES (?,?,?,?)",
        UUID.randomUUID(),
        classId,
        student,
        Timestamp.from(joinedAt));
  }

  private void assign(UUID classId, UUID forProblem, Instant assignedAt) {
    jdbc.update(
        "INSERT INTO problem.class_assignments(id,problem_id,class_id,assigned_by,assigned_at)"
            + " VALUES (?,?,?,?,?)",
        UUID.randomUUID(),
        forProblem,
        classId,
        instructor,
        Timestamp.from(assignedAt));
  }

  private String envelope(UUID eventId, UUID student, UUID submission) {
    return "{\"eventId\":\""
        + eventId
        + "\",\"eventType\":\"SubmissionAccepted\",\"occurredAt\":\""
        + HAPPENED
        + "\",\"schemaVersion\":1,\"payload\":{\"submissionId\":\""
        + submission
        + "\",\"userId\":\""
        + student
        + "\",\"problemId\":\""
        + problem
        + "\",\"language\":\"PYTHON\"}}";
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

  private List<Map<String, Object>> feed(UUID student) {
    return jdbc.queryForList(
        "SELECT scope_class_id, activity_type, ref_type, ref_id, occurred_at"
            + " FROM identity.identity_recent_activity WHERE actor_user_id = ?",
        student);
  }

  @Test
  void theEntryLandsInTheClassWhereTheProblemWasAssignedAndARedeliveryAddsNothing() {
    UUID student = user("STUDENT");
    UUID classId = classroom();
    UUID submission = UUID.randomUUID();
    UUID eventId = UUID.randomUUID();
    enroll(classId, student, BEFORE);
    assign(classId, problem, BEFORE);

    publish(envelope(eventId, student, submission));
    awaitProcessed(eventId);
    publish(envelope(eventId, student, submission));
    UUID marker = UUID.randomUUID();
    publish(envelope(marker, user("STUDENT"), UUID.randomUUID()));
    awaitProcessed(marker);

    List<Map<String, Object>> entries = feed(student);
    assertThat(entries).hasSize(1);
    Map<String, Object> entry = entries.getFirst();
    assertThat(entry.get("scope_class_id")).isEqualTo(classId);
    assertThat(entry.get("activity_type")).isEqualTo("SUBMISSION_ACCEPTED");
    assertThat(entry.get("ref_type")).isEqualTo("SUBMISSION");
    assertThat(entry.get("ref_id")).isEqualTo(submission);
    assertThat(((Timestamp) entry.get("occurred_at")).toInstant()).isEqualTo(HAPPENED);
  }

  @Test
  void aProblemAssignedOnlyToAnotherClassOrAStudentWhoJoinedLaterGivesNoClass() {
    UUID withoutAssignment = user("STUDENT");
    UUID joinedLater = user("STUDENT");
    UUID classId = classroom();
    UUID otherClass = classroom();
    enroll(classId, withoutAssignment, BEFORE);
    assign(otherClass, problem, BEFORE);
    enroll(otherClass, joinedLater, Instant.parse("2026-11-03T11:00:00Z"));
    UUID first = UUID.randomUUID();
    UUID second = UUID.randomUUID();

    publish(envelope(first, withoutAssignment, UUID.randomUUID()));
    publish(envelope(second, joinedLater, UUID.randomUUID()));
    awaitProcessed(first);
    awaitProcessed(second);

    assertThat(feed(withoutAssignment)).hasSize(1);
    assertThat(feed(withoutAssignment).getFirst().get("scope_class_id")).isNull();
    assertThat(feed(joinedLater)).hasSize(1);
    assertThat(feed(joinedLater).getFirst().get("scope_class_id")).isNull();
  }

  @Test
  void aMalformedMessageIsDroppedAndTheConsumerKeepsWorking() {
    publish("{\"eventId\":\"not-a-uuid\"}");
    UUID student = user("STUDENT");
    UUID after = UUID.randomUUID();
    publish(envelope(after, student, UUID.randomUUID()));

    awaitProcessed(after);

    assertThat(feed(student)).hasSize(1);
  }
}
