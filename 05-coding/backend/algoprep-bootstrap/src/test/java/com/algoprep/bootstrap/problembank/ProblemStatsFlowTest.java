package com.algoprep.bootstrap.problembank;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import com.algoprep.identity.application.ports.out.AccessTokenPort;
import com.algoprep.identity.domain.model.BaseCategory;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.MountableFile;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * F2-2f: the {@code SubmissionGraded} consumer of problem-bank (DD F2 section 59.2, BR-20) with a
 * real message through RabbitMQ into PostgreSQL, and endpoint #40 (DD F2 section 41) reading what
 * the consumer wrote. The envelope is built by hand in the shape of DD api F4 section 25.2.
 */
// An empty exclude list replaces the one of the test profile that switches the broker off.
@SpringBootTest(
    properties = {"spring.autoconfigure.exclude=", "spring.datasource.hikari.maximum-pool-size=4"})
@ActiveProfiles("test")
@Testcontainers
class ProblemStatsFlowTest {
  private static final JsonMapper JSON = JsonMapper.builder().build();
  private static final String BASE = "/api/v1/problem-bank/authoring/problems/";

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
          .withEnv("RABBITMQ_DEFAULT_USER", "test-stats-user")
          .withEnv("RABBITMQ_DEFAULT_PASS", "test-stats-password")
          .withExposedPorts(5672)
          .waitingFor(Wait.forLogMessage(".*Server startup complete.*", 1));

  @DynamicPropertySource
  static void infrastructure(DynamicPropertyRegistry properties) {
    properties.add("spring.data.redis.host", REDIS::getHost);
    properties.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
    properties.add("spring.rabbitmq.host", RABBIT::getHost);
    properties.add("spring.rabbitmq.port", () -> RABBIT.getMappedPort(5672));
    properties.add("spring.rabbitmq.username", () -> "test-stats-user");
    properties.add("spring.rabbitmq.password", () -> "test-stats-password");
  }

  @Autowired WebApplicationContext context;
  @Autowired JdbcTemplate jdbc;
  @Autowired AccessTokenPort tokens;
  @Autowired RabbitTemplate rabbit;

  private MockMvc mvc;
  private UUID teacherId;
  private UUID rivalId;
  private String teacher;
  private String rival;
  private String admin;
  private String student;
  private String levelId;

  @BeforeEach
  void setup() {
    mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    jdbc.update("DELETE FROM problem.problem_stats");
    jdbc.update("DELETE FROM problem.consumed_events");
    jdbc.update("DELETE FROM problem.class_assignments");
    jdbc.update("DELETE FROM problem.problem_topics");
    jdbc.update("DELETE FROM problem.problems");
    levelId =
        jdbc.queryForObject(
            "SELECT id::text FROM problem.problem_levels ORDER BY sort_order LIMIT 1",
            String.class);
    teacherId = UUID.randomUUID();
    rivalId = UUID.randomUUID();
    teacher = token(teacherId, "INSTRUCTOR", BaseCategory.INSTRUCTOR);
    rival = token(rivalId, "INSTRUCTOR", BaseCategory.INSTRUCTOR);
    admin = token(UUID.randomUUID(), "ADMIN", BaseCategory.ADMIN);
    student = token(UUID.randomUUID(), "STUDENT", BaseCategory.STUDENT);
  }

  @Test
  void aRealMessageUpdatesTheCountersAndARedeliveryOfTheSameEventIdCountsOnce() throws Exception {
    var problem = problem(teacherId, "PUBLISHED");
    var accepted = UUID.randomUUID();
    publish(event(accepted, problem, "ACCEPTED"));
    publish(event(UUID.randomUUID(), problem, "WRONG_ANSWER"));
    publish(event(UUID.randomUUID(), problem, "SYSTEM_ERROR"));
    publish(event(accepted, problem, "ACCEPTED")); // the same event id delivered again
    publish(event(UUID.randomUUID(), UUID.randomUUID(), "ACCEPTED")); // unknown problem
    publish("{\"eventId\":\"not-a-uuid\"}"); // dropped, the consumer keeps going
    var marker = UUID.randomUUID();
    publish(event(marker, problem, "WRONG_ANSWER"));
    awaitConsumed(marker);

    // accepted, wrong answer and the marker count; the system error and the duplicate do not.
    var row = jdbc.queryForMap("SELECT * FROM problem.problem_stats WHERE problem_id = ?", problem);
    assertThat(row.get("submission_count")).isEqualTo(3);
    assertThat(row.get("accepted_count")).isEqualTo(1);
    assertThat(row.get("ac_rate").toString()).isEqualTo("33.33");
    assertThat(jdbc.queryForObject("SELECT count(*) FROM problem.problem_stats", Integer.class))
        .isEqualTo(1);
    // Every distinct event id is recorded once, including the system error and the unknown problem.
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM problem.consumed_events WHERE event_id = ?",
                Integer.class,
                accepted))
        .isEqualTo(1);
    assertThat(jdbc.queryForObject("SELECT count(*) FROM problem.consumed_events", Integer.class))
        .isEqualTo(5);

    var stats = json(call(admin, problem.toString()));
    assertThat(stats.path("submissionCount").asInt()).isEqualTo(3);
    assertThat(stats.path("acRate").decimalValue()).isEqualByComparingTo("33.33");
    assertThat(stats.path("updatedAt").asString()).isNotBlank();
  }

  @Test
  void aProblemNobodySubmittedToAnswersZeroAndNullsAndTheAccessRulesMatchSection34()
      throws Exception {
    var fresh = problem(teacherId, "UNPUBLISHED");
    var body = json(call(teacher, fresh.toString()));
    assertThat(body.path("submissionCount").asInt()).isZero();
    assertThat(body.path("acRate").isNull()).isTrue();
    assertThat(body.path("updatedAt").isNull()).isTrue();

    // Someone else's draft is the same 404 as a missing problem, and a published one is readable.
    assertThat(call(rival, fresh.toString()).getStatus()).isEqualTo(404);
    assertThat(call(admin, fresh.toString()).getStatus()).isEqualTo(200);
    jdbc.update("UPDATE problem.problems SET status = 'PUBLISHED' WHERE id = ?", fresh);
    assertThat(call(rival, fresh.toString()).getStatus()).isEqualTo(200);

    var missing = call(admin, UUID.randomUUID().toString());
    assertThat(missing.getStatus()).isEqualTo(404);
    assertThat(json(missing).path("errorCode").asString()).isEqualTo("PRB-010");
    assertThat(call(admin, "not-a-uuid").getStatus()).isEqualTo(404);

    jdbc.update(
        "UPDATE problem.problems SET status = 'UNPUBLISHED', deleted = true WHERE id = ?", fresh);
    assertThat(call(admin, fresh.toString()).getStatus()).isEqualTo(404);

    assertThat(call(student, fresh.toString()).getStatus()).isEqualTo(403);
    assertThat(call(null, fresh.toString()).getStatus()).isEqualTo(401);
  }

  private void publish(Object body) {
    rabbit.send(
        "algoprep.domain-events",
        "judge.submission.graded",
        MessageBuilder.withBody(body.toString().getBytes(StandardCharsets.UTF_8))
            .setContentType("application/json")
            .build());
  }

  private static String event(UUID eventId, UUID problem, String verdict) {
    return "{\"eventId\":\""
        + eventId
        + "\",\"eventType\":\"SubmissionGraded\",\"occurredAt\":\"2026-11-06T00:00:00Z\","
        + "\"schemaVersion\":1,\"payload\":{\"submissionId\":\""
        + UUID.randomUUID()
        + "\",\"userId\":\""
        + UUID.randomUUID()
        + "\",\"problemId\":\""
        + problem
        + "\",\"language\":\"JAVA\",\"verdict\":\""
        + verdict
        + "\",\"passedCount\":null,\"totalCount\":null,\"passedTestcaseRatio\":null,"
        + "\"submittedAt\":\"2026-11-06T00:00:00Z\",\"firstAttemptForProblem\":true}}";
  }

  /** Polls for up to 15 seconds; the consumer runs on the listener container's own thread. */
  private void awaitConsumed(UUID eventId) {
    long deadline = System.nanoTime() + 15_000_000_000L;
    while (jdbc.queryForObject(
            "SELECT count(*) FROM problem.consumed_events WHERE event_id = ?",
            Integer.class,
            eventId)
        != 1) {
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

  private MockHttpServletResponse call(String token, String problemId) throws Exception {
    var request = get(BASE + problemId + "/stats");
    if (token != null) {
      request.header("Authorization", "Bearer " + token);
    }
    return mvc.perform(request).andReturn().getResponse();
  }

  private static JsonNode json(MockHttpServletResponse response) throws Exception {
    return JSON.readTree(response.getContentAsString());
  }

  private String token(UUID id, String role, BaseCategory category) {
    jdbc.update(
        "INSERT INTO identity.users(id,email,display_name,role_id,status) SELECT ?,?,'Stats actor',id,'ACTIVE' FROM identity.roles WHERE code=?",
        id,
        id + "@test.local",
        role);
    return tokens.issue(id, category, role, UUID.randomUUID()).value();
  }

  private UUID problem(UUID author, String status) {
    var id = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO problem.problems(id,code,title,statement_md,author_id,level_id,status,time_limit_ms,memory_limit_mb,max_output_size_kb) VALUES (?,?,'Stats fixture','s',?,?::uuid,?,1000,256,64)",
        id,
        id.toString(),
        author,
        levelId,
        status);
    return id;
  }
}
