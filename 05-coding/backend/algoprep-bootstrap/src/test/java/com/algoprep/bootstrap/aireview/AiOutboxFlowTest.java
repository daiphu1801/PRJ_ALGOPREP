package com.algoprep.bootstrap.aireview;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.algoprep.aireview.application.ports.in.CleanAiOutboxUseCase;
import com.algoprep.aireview.application.ports.in.RelayAiOutboxUseCase;
import com.algoprep.aireview.application.ports.out.AiDomainEventPort;
import com.algoprep.aireview.domain.model.AiOutboxEvent;
import com.algoprep.aireview.domain.model.SubmissionSnapshot;
import com.algoprep.aireview.domain.ports.out.AiOutboxRepository;
import com.algoprep.identity.application.ports.out.AccessTokenPort;
import com.algoprep.identity.domain.model.BaseCategory;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.MountableFile;

/**
 * F5-5a: the SolutionReviewCompleted outbox on real PostgreSQL, Redis and RabbitMQ. Covers the
 * writer (same transaction as the review, cache hit included, rollback), JOB-AI-01 (confirm,
 * mandatory return, retry without loss, SKIP LOCKED), JOB-AI-02 and the identity consumer end to
 * end (AC-52, AC-58). The relay is driven by hand: the scheduled jobs are off.
 */
@SpringBootTest(properties = {"spring.autoconfigure.exclude=", "ai.outbox.enabled=false"})
@ActiveProfiles("test")
@Testcontainers
@Import({SolutionReviewFlowTest.Doubles.class, AiOutboxFlowTest.Transport.class})
class AiOutboxFlowTest {
  private static final String QUEUE = "ai-outbox-flow-test";
  private static final String GOOD =
      """
      {"approachTitle":"Hash map","verdict":"IMPROVABLE",
       "complexity":{"actualTime":"O(n)","timeExplanation":"one pass",
       "actualSpace":"O(n)","spaceExplanation":"a map"},
       "strengths":["clear"],"improvements":["less memory"],"edgeCases":["empty"],
       "followUpTopics":["hashing"],"readabilityScore":4,
       "codeDiff":{"suggestedCode":"print(2)","explanation":"shorter"}}
      """;

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
          .withEnv("RABBITMQ_DEFAULT_USER", "test-ai-user")
          .withEnv("RABBITMQ_DEFAULT_PASS", "test-ai-password")
          .withExposedPorts(5672)
          .waitingFor(Wait.forLogMessage(".*Server startup complete.*", 1));

  @DynamicPropertySource
  static void infrastructure(DynamicPropertyRegistry properties) {
    properties.add("spring.data.redis.host", REDIS::getHost);
    properties.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
    properties.add("spring.data.redis.timeout", () -> "2s");
    properties.add("spring.rabbitmq.host", RABBIT::getHost);
    properties.add("spring.rabbitmq.port", () -> RABBIT.getMappedPort(5672));
    properties.add("spring.rabbitmq.username", () -> "test-ai-user");
    properties.add("spring.rabbitmq.password", () -> "test-ai-password");
  }

  @Autowired WebApplicationContext context;
  @Autowired AccessTokenPort tokens;
  @Autowired JdbcTemplate jdbc;
  @Autowired DataSource dataSource;
  @Autowired StringRedisTemplate redis;
  @Autowired RabbitTemplate rabbit;
  @Autowired RelayAiOutboxUseCase relay;
  @Autowired CleanAiOutboxUseCase cleanup;
  @Autowired ControlledPublisher publisher;
  @Autowired SolutionReviewFlowTest.ScriptedLlm llm;
  @Autowired SolutionReviewFlowTest.ScriptedSubmissions submissions;
  @MockitoSpyBean AiOutboxRepository outbox;

  private MockMvc mvc;
  private UUID userId;
  private UUID problemId;
  private String student;

  @BeforeEach
  void reset() {
    mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    jdbc.update("TRUNCATE ai.ai_cache_entries, ai.solution_reviews, ai.token_usage CASCADE");
    jdbc.update("DELETE FROM ai.outbox_events");
    jdbc.update("TRUNCATE ai.rubric_scores, ai.interview_turns, ai.interview_sessions CASCADE");
    jdbc.update(
        "UPDATE ai.ai_token_budget_configs SET limit_tokens = 0, locked_at = NULL,"
            + " period_start = DATE '2026-11-01', review_per_hour = 10, cooldown_seconds = 20"
            + " WHERE id = 1");
    llm.script.clear();
    submissions.accepted.clear();
    publisher.entered = null;
    publisher.release = null;
    publisher.fail = false;
    publisher.seen.clear();
    rabbit.execute(
        channel -> {
          channel.exchangeDeclare("algoprep.domain-events", "topic", true);
          channel.queueDeclare(QUEUE, false, false, false, null);
          channel.queuePurge(QUEUE);
          channel.queueBind(QUEUE, "algoprep.domain-events", "ai.solution-review.*");
          channel.queueBind(QUEUE, "algoprep.domain-events", "ai.interview-session.*");
          return null;
        });
    userId = UUID.randomUUID();
    problemId = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO identity.users(id,email,display_name,role_id,status) SELECT ?,?,"
            + " 'Outbox learner',id,'ACTIVE' FROM identity.roles WHERE code='STUDENT'",
        userId,
        userId + "@test.local");
    student = tokens.issue(userId, BaseCategory.STUDENT, "STUDENT", UUID.randomUUID()).value();
  }

  // Writer ------------------------------------------------------------------------------------

  @Test
  void aGeneratedReviewWritesExactlyOnePendingEventInTheSameTransaction() throws Exception {
    UUID sub = submission("print(1)");
    llm.ok(GOOD);

    var response = review(sub);
    assertThat(response.getStatus()).as(response.getContentAsString()).isEqualTo(200);

    Map<String, Object> row = jdbc.queryForMap("SELECT * FROM ai.outbox_events");
    UUID reviewId = jdbc.queryForObject("SELECT id FROM ai.solution_reviews", UUID.class);
    assertThat(row.get("aggregate_type")).isEqualTo("SolutionReview");
    assertThat(row.get("aggregate_id")).isEqualTo(reviewId);
    assertThat(row.get("event_type")).isEqualTo("SolutionReviewCompleted");
    assertThat(row.get("published_at")).isNull();
    assertThat(row.get("publish_attempts")).isEqualTo(0);
    // Ids and the score only: no source code, no review text.
    String payload = row.get("payload").toString();
    assertThat(payload)
        .contains(reviewId.toString(), sub.toString(), userId.toString(), problemId.toString())
        .contains("\"readabilityScore\": 4")
        .doesNotContain("print(", "Hash map", "clear");
  }

  @Test
  void aCacheHitEmitsItsOwnEventForTheNewReview() throws Exception {
    UUID first = submission("print(1)");
    llm.ok(GOOD);
    assertThat(review(first).getStatus()).isEqualTo(200);
    redis.delete("ai:cooldown:" + userId);

    UUID second = submission("print(1)");
    var hit = review(second);
    assertThat(hit.getStatus()).as(hit.getContentAsString()).isEqualTo(200);

    assertThat(count("ai.solution_reviews")).isEqualTo(2);
    assertThat(
            jdbc.queryForObject(
                "SELECT served_from_cache FROM ai.solution_reviews WHERE submission_id = ?",
                Boolean.class,
                second))
        .isTrue();
    assertThat(count("ai.outbox_events")).isEqualTo(2);
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM ai.outbox_events o JOIN ai.solution_reviews r"
                    + " ON r.id = o.aggregate_id WHERE r.submission_id = ?",
                Long.class,
                second))
        .isEqualTo(1);
  }

  @Test
  void aFailureAfterTheOutboxInsertRollsTheReviewBackWithIt() {
    org.mockito.Mockito.doAnswer(
            invocation -> {
              invocation.callRealMethod();
              throw new IllegalStateException("forced failure after the outbox insert");
            })
        .when(outbox)
        .insert(
            org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.any());
    UUID sub = submission("print(1)");
    llm.ok(GOOD);

    try {
      review(sub);
    } catch (Exception expected) {
      // The unhandled failure may surface as an exception or a 5xx; only the database matters.
    }

    assertThat(count("ai.solution_reviews")).isZero();
    assertThat(count("ai.ai_cache_entries")).isZero();
    assertThat(count("ai.outbox_events")).isZero();
  }

  // Relay -------------------------------------------------------------------------------------

  @Test
  void theRelayPublishesWithAConfirmAndMarksTheRowOnlyOnce() throws Exception {
    llm.ok(GOOD);
    UUID sub = submission("print(2)");
    assertThat(review(sub).getStatus()).isEqualTo(200);
    UUID eventId = jdbc.queryForObject("SELECT id FROM ai.outbox_events", UUID.class);

    assertThat(relay.relay()).isEqualTo(1);

    assertThat(pending()).isZero();
    assertThat(attempts(eventId)).isEqualTo(1);
    Message message = rabbit.receive(QUEUE, 5000);
    assertThat(message).isNotNull();
    assertThat(message.getMessageProperties().getMessageId()).isEqualTo(eventId.toString());
    String body = new String(message.getBody(), StandardCharsets.UTF_8);
    assertThat(body)
        .contains("\"eventId\":\"" + eventId + "\"")
        .contains("\"eventType\":\"SolutionReviewCompleted\"")
        .contains("\"schemaVersion\":1")
        .contains("\"userId\":\"" + userId + "\"");
    assertThat(relay.relay()).isZero();
    assertThat(rabbit.receive(QUEUE, 500)).isNull();
  }

  @Test
  void anUnconfirmedOrUnroutableRowStaysPendingAndIsDeliveredOnceLater() {
    UUID id = seed("SolutionReview", "SolutionReviewCompleted");
    publisher.fail = true;
    assertThat(relay.relay()).isEqualTo(1);
    assertThat(pending()).isEqualTo(1);
    assertThat(attempts(id)).isEqualTo(1);
    assertThat(rabbit.receive(QUEUE, 300)).isNull();

    publisher.fail = false;
    assertThat(relay.relay()).isEqualTo(1);
    assertThat(pending()).isZero();
    assertThat(attempts(id)).isEqualTo(2);
    assertThat(rabbit.receive(QUEUE, 5000)).isNotNull();
    // Nothing lost and nothing duplicated: one message for one row.
    assertThat(rabbit.receive(QUEUE, 300)).isNull();
  }

  @Test
  void aKeyWithNoBoundQueueIsReturnedByTheBrokerSoTheRowStaysPending() {
    // The identity queue of this key exists since F5-8; unbind the test queue and the identity
    // queue for the duration of the test so the key has no destination.
    unbindInterviewKey(false);
    try {
      UUID id = seed("InterviewSession", "MockInterviewFinished");
      assertThat(relay.relay()).isEqualTo(1);
      assertThat(pending()).isEqualTo(1);
      assertThat(attempts(id)).isEqualTo(1);
    } finally {
      unbindInterviewKey(true);
    }
  }

  private void unbindInterviewKey(boolean bind) {
    rabbit.execute(
        channel -> {
          for (String queue : List.of(QUEUE, "identity.mock-interview-finished")) {
            String key =
                queue.equals(QUEUE) ? "ai.interview-session.*" : "ai.interview-session.finished";
            if (bind) {
              channel.queueBind(queue, "algoprep.domain-events", key);
            } else {
              channel.queueUnbind(queue, "algoprep.domain-events", key);
            }
          }
          return null;
        });
  }

  @Test
  void aSecondRelaySkipsRowsLockedByTheFirst() throws Exception {
    UUID id = seed("SolutionReview", "SolutionReviewCompleted");
    publisher.entered = new CountDownLatch(1);
    publisher.release = new CountDownLatch(1);
    try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
      var first = executor.submit(relay::relay);
      try {
        assertThat(publisher.entered.await(10, TimeUnit.SECONDS)).isTrue();
        assertThat(relay.relay()).isZero();
      } finally {
        publisher.release.countDown();
      }
      assertThat(first.get(15, TimeUnit.SECONDS)).isEqualTo(1);
    }
    assertThat(attempts(id)).isEqualTo(1);
    assertThat(pending()).isZero();
  }

  @Test
  void theBatchIsBoundedAtOneHundred() {
    for (int i = 0; i < 101; i++) {
      seed("SolutionReview", "SolutionReviewCompleted");
    }
    assertThat(relay.relay()).isEqualTo(100);
    assertThat(pending()).isEqualTo(1);
  }

  // Cleanup -----------------------------------------------------------------------------------

  @Test
  void cleanupRemovesOnlyPublishedRowsPastNinetyDays() {
    UUID oldPublished = seed("SolutionReview", "SolutionReviewCompleted");
    UUID freshPublished = seed("SolutionReview", "SolutionReviewCompleted");
    UUID oldPending = seed("SolutionReview", "SolutionReviewCompleted");
    jdbc.update(
        "UPDATE ai.outbox_events SET published_at = now() - interval '91 days' WHERE id=?",
        oldPublished);
    jdbc.update(
        "UPDATE ai.outbox_events SET published_at = now() - interval '89 days' WHERE id=?",
        freshPublished);
    jdbc.update(
        "UPDATE ai.outbox_events SET created_at = now() - interval '400 days' WHERE id=?",
        oldPending);

    assertThat(cleanup.clean()).isEqualTo(1);

    assertThat(exists(oldPublished)).isFalse();
    assertThat(exists(freshPublished)).isTrue();
    assertThat(exists(oldPending)).isTrue();
    assertThat(cleanup.clean()).isZero();
  }

  @Test
  void cleanupDoesNothingWhileAnotherInstanceHoldsTheLock() throws Exception {
    UUID old = seed("SolutionReview", "SolutionReviewCompleted");
    jdbc.update(
        "UPDATE ai.outbox_events SET published_at = now() - interval '100 days' WHERE id=?", old);
    try (Connection other = dataSource.getConnection()) {
      other.setAutoCommit(false);
      try (var statement = other.createStatement()) {
        statement.execute("SELECT pg_advisory_xact_lock(hashtextextended('ai:outbox:cleanup', 0))");
        assertThat(cleanup.clean()).isZero();
        assertThat(exists(old)).isTrue();
      } finally {
        other.rollback();
      }
    }
    assertThat(cleanup.clean()).isEqualTo(1);
  }

  // End to end with the identity consumer --------------------------------------------------------

  @Test
  void theIdentityFeedGetsOneEntryEvenWhenTheMessageIsDeliveredTwice() throws Exception {
    llm.ok(GOOD);
    assertThat(review(submission("print(3)")).getStatus()).isEqualTo(200);
    UUID eventId = jdbc.queryForObject("SELECT id FROM ai.outbox_events", UUID.class);
    UUID reviewId = jdbc.queryForObject("SELECT id FROM ai.solution_reviews", UUID.class);

    assertThat(relay.relay()).isEqualTo(1);
    Message copy = rabbit.receive(QUEUE, 5000);
    assertThat(copy).isNotNull();
    awaitProcessed(eventId);
    // The relay is at-least-once: replay the identical message and the feed must not grow.
    rabbit.send("algoprep.domain-events", "ai.solution-review.completed", copy);
    Thread.sleep(1000);

    List<Map<String, Object>> feed =
        jdbc.queryForList(
            "SELECT actor_user_id, scope_class_id, activity_type, ref_type FROM"
                + " identity.identity_recent_activity WHERE ref_id = ?",
            reviewId);
    assertThat(feed).hasSize(1);
    assertThat(feed.getFirst().get("actor_user_id")).isEqualTo(userId);
    assertThat(feed.getFirst().get("activity_type")).isEqualTo("SOLUTION_REVIEW_COMPLETED");
    assertThat(feed.getFirst().get("scope_class_id")).isNull();
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM identity.processed_domain_events WHERE event_id = ?",
                Integer.class,
                eventId))
        .isEqualTo(1);
  }

  @Test
  void aFinishedInterviewReachesTheIdentityFeedOnceAndLeavesNoPendingRow() throws Exception {
    UUID session = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO ai.interview_sessions(id,user_id,entry_type,topics,interviewer_level,"
            + "max_turns,hint_allowed,stage,turn_count,started_at,updated_at) VALUES"
            + " (?,?,'TOPIC_SELECTION',CAST('[\"graphs\"]' AS jsonb),'JUNIOR',8,true,'CHALLENGE',2,"
            + "now(),now())",
        session,
        userId);
    insertTranscript(session);
    llm.script.add(
        new com.algoprep.aireview.domain.model.StructuredResult(
            "{\"criteria\":[{\"code\":\"CLARITY\",\"score\":8,\"comment\":\"a\"},"
                + "{\"code\":\"TECHNICAL_ACCURACY\",\"score\":6,\"comment\":\"b\"},"
                + "{\"code\":\"PUSHBACK_HANDLING\",\"score\":7,\"comment\":\"c\"},"
                + "{\"code\":\"COMPLEXITY_AWARENESS\",\"score\":5,\"comment\":\"d\"}]}",
            new com.algoprep.aireview.domain.model.LlmUsage(10, 5),
            "scripted-model"));

    var done =
        mvc.perform(
                post("/api/v1/ai/interview-sessions/" + session + "/complete")
                    .header("Authorization", "Bearer " + student))
            .andReturn()
            .getResponse();
    assertThat(done.getStatus()).as(done.getContentAsString()).isEqualTo(200);
    UUID eventId = jdbc.queryForObject("SELECT id FROM ai.outbox_events", UUID.class);
    assertThat(pending()).isEqualTo(1);

    assertThat(relay.relay()).isEqualTo(1);
    Message copy = rabbit.receive(QUEUE, 5000);
    assertThat(copy).isNotNull();
    awaitProcessed(eventId);
    rabbit.send("algoprep.domain-events", "ai.interview-session.finished", copy);
    Thread.sleep(1000);

    assertThat(pending()).isZero();
    List<Map<String, Object>> feed =
        jdbc.queryForList(
            "SELECT actor_user_id, scope_class_id, activity_type, ref_type FROM"
                + " identity.identity_recent_activity WHERE ref_id = ?",
            session);
    assertThat(feed).hasSize(1);
    assertThat(feed.getFirst().get("actor_user_id")).isEqualTo(userId);
    assertThat(feed.getFirst().get("activity_type")).isEqualTo("MOCK_INTERVIEW_FINISHED");
    assertThat(feed.getFirst().get("ref_type")).isEqualTo("INTERVIEW_SESSION");
    assertThat(feed.getFirst().get("scope_class_id")).isNull();
  }

  // Helpers -----------------------------------------------------------------------------------

  private void insertTranscript(UUID session) {
    int index = 1;
    for (String[] line :
        new String[][] {{"AI", "Explain."}, {"USER", "It is a BFS."}, {"AI", "Why BFS?"}}) {
      jdbc.update(
          "INSERT INTO ai.interview_turns(id,interview_session_id,turn_index,role,stage_at_turn,"
              + "content) VALUES (?,?,?,?,'EXPLAIN',?)",
          UUID.randomUUID(),
          session,
          index++,
          line[0],
          line[1]);
    }
  }

  private UUID submission(String code) {
    UUID id = UUID.randomUUID();
    submissions.accepted.put(id, new SubmissionSnapshot(id, userId, problemId, "PYTHON", code));
    return id;
  }

  private MockHttpServletResponse review(UUID submission) throws Exception {
    return mvc.perform(
            post("/api/v1/ai/submissions/" + submission + "/solution-review")
                .header("Authorization", "Bearer " + student))
        .andReturn()
        .getResponse();
  }

  private UUID seed(String aggregateType, String eventType) {
    UUID id = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO ai.outbox_events(id,aggregate_type,aggregate_id,event_type,payload)"
            + " VALUES (?,?,?,?,CAST(? AS jsonb))",
        id,
        aggregateType,
        UUID.randomUUID(),
        eventType,
        "{\"userId\":\"" + userId + "\",\"reviewId\":\"" + UUID.randomUUID() + "\"}");
    return id;
  }

  private void awaitProcessed(UUID eventId) throws InterruptedException {
    long deadline = System.nanoTime() + 15_000_000_000L;
    while (jdbc.queryForObject(
            "SELECT count(*) FROM identity.processed_domain_events WHERE event_id = ?",
            Integer.class,
            eventId)
        != 1) {
      if (System.nanoTime() > deadline) {
        throw new AssertionError("event " + eventId + " was not consumed in time");
      }
      Thread.sleep(100);
    }
  }

  private long count(String table) {
    return jdbc.queryForObject("SELECT count(*) FROM " + table, Long.class);
  }

  private int pending() {
    return jdbc.queryForObject(
        "SELECT count(*) FROM ai.outbox_events WHERE published_at IS NULL", Integer.class);
  }

  private int attempts(UUID id) {
    return jdbc.queryForObject(
        "SELECT publish_attempts FROM ai.outbox_events WHERE id=?", Integer.class, id);
  }

  private boolean exists(UUID id) {
    return jdbc.queryForObject(
            "SELECT count(*) FROM ai.outbox_events WHERE id=?", Integer.class, id)
        > 0;
  }

  @TestConfiguration
  static class Transport {
    @Bean
    @Primary
    ControlledPublisher controlledAiPublisher(
        @Qualifier("rabbitAiDomainEventAdapter") AiDomainEventPort actual) {
      return new ControlledPublisher(actual);
    }
  }

  static class ControlledPublisher implements AiDomainEventPort {
    private final AiDomainEventPort actual;
    volatile CountDownLatch entered;
    volatile CountDownLatch release;
    volatile boolean fail;
    final java.util.List<UUID> seen = new java.util.concurrent.CopyOnWriteArrayList<>();

    ControlledPublisher(AiDomainEventPort actual) {
      this.actual = actual;
    }

    @Override
    public boolean publish(AiOutboxEvent event, String envelope) {
      seen.add(event.id());
      if (entered != null) {
        entered.countDown();
        try {
          if (!release.await(15, TimeUnit.SECONDS)) {
            throw new IllegalStateException("barrier timeout");
          }
        } catch (InterruptedException interrupted) {
          Thread.currentThread().interrupt();
          throw new IllegalStateException("interrupted");
        }
      }
      if (fail) {
        throw new IllegalStateException("broker unavailable");
      }
      return actual.publish(event, envelope);
    }
  }
}
