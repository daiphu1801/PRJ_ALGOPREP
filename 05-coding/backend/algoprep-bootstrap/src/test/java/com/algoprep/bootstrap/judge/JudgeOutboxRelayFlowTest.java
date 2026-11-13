package com.algoprep.bootstrap.judge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.algoprep.judge.application.ports.in.RelayJudgeOutboxUseCase;
import com.algoprep.judge.application.ports.out.JudgeDomainEventPort;
import com.algoprep.judge.domain.model.OutboxEvent;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.MountableFile;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest(properties = {"spring.autoconfigure.exclude=", "judge.outbox.enabled=false"})
@ActiveProfiles("test")
@Testcontainers
@Import(JudgeOutboxRelayFlowTest.Transport.class)
class JudgeOutboxRelayFlowTest {
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
          .withEnv("RABBITMQ_DEFAULT_USER", "test-relay-user")
          .withEnv("RABBITMQ_DEFAULT_PASS", "test-relay-password")
          .withExposedPorts(5672)
          .waitingFor(Wait.forLogMessage(".*Server startup complete.*", 1));

  @DynamicPropertySource
  static void infrastructure(DynamicPropertyRegistry properties) {
    properties.add("spring.data.redis.host", REDIS::getHost);
    properties.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
    properties.add("spring.rabbitmq.host", RABBIT::getHost);
    properties.add("spring.rabbitmq.port", () -> RABBIT.getMappedPort(5672));
    properties.add("spring.rabbitmq.username", () -> "test-relay-user");
    properties.add("spring.rabbitmq.password", () -> "test-relay-password");
  }

  @Autowired JdbcTemplate jdbc;
  @Autowired RelayJudgeOutboxUseCase relay;
  @Autowired ControlledPublisher publisher;
  @Autowired PlatformTransactionManager transactions;
  @Autowired org.springframework.amqp.rabbit.core.RabbitTemplate rabbit;
  private final JsonMapper json = JsonMapper.builder().build();

  @BeforeEach
  void reset() {
    jdbc.update("DELETE FROM judge.outbox_events");
    publisher.fail.set(false);
    publisher.entered = null;
    publisher.release = null;
  }

  @Test
  void retryPastErrorThresholdThenRealRabbitUpdatesF1ExactlyOnce() throws Exception {
    UUID user = UUID.randomUUID();
    UUID graded = seed(user, "SubmissionGraded");
    UUID accepted = seed(user, "SubmissionAccepted");
    publisher.fail.set(true);
    for (int i = 0; i < 6; i++) {
      assertThat(relay.relay()).isEqualTo(2);
    }
    assertThat(pending()).isEqualTo(2);
    assertThat(attempts(graded)).isEqualTo(6);
    publisher.fail.set(false);
    assertThat(relay.relay()).isEqualTo(2);
    awaitProcessed(graded);
    awaitProcessed(accepted);
    assertThat(pending()).isZero();
    assertThat(attempts(graded)).isEqualTo(7);
    assertThat(total(user)).isEqualTo(1);
    // Reproduce a lost DB commit after broker confirmation using the original envelope.
    jdbc.update("UPDATE judge.outbox_events SET published_at=NULL WHERE id=?", graded);
    assertThat(relay.relay()).isEqualTo(1);
    UUID marker = seed(UUID.randomUUID(), "SubmissionGraded");
    relay.relay();
    awaitProcessed(marker);
    assertThat(total(user)).isEqualTo(1);
    assertThat(
            jdbc.queryForObject(
                "SELECT submission_count FROM identity.user_daily_submission_stats WHERE user_id=? AND day='2026-11-03'",
                Integer.class,
                user))
        .isEqualTo(1);
    assertThat(relay.relay()).isZero();
  }

  @Test
  void unroutableBrokerAckDoesNotLoseTheEvent() throws Exception {
    UUID id = seed(UUID.randomUUID(), "SubmissionGraded");
    rabbit.execute(
        channel -> {
          channel.queueUnbind(
              "identity.submission-graded", "algoprep.domain-events", "judge.submission.graded");
          // F2 also consumes this event, so its queue must be unbound for the key to be unroutable.
          channel.queueUnbind(
              "problem-bank.submission-graded",
              "algoprep.domain-events",
              "judge.submission.graded");
          return null;
        });
    try {
      assertThat(relay.relay()).isEqualTo(1);
      assertThat(pending()).isEqualTo(1);
      assertThat(attempts(id)).isEqualTo(1);
    } finally {
      rabbit.execute(
          channel -> {
            channel.queueBind(
                "identity.submission-graded", "algoprep.domain-events", "judge.submission.graded");
            channel.queueBind(
                "problem-bank.submission-graded",
                "algoprep.domain-events",
                "judge.submission.graded");
            return null;
          });
    }
    assertThat(relay.relay()).isEqualTo(1);
    awaitProcessed(id);
    assertThat(pending()).isZero();
  }

  @Test
  void secondRelaySkipsRowsLockedByTheFirst() throws Exception {
    UUID event = seed(UUID.randomUUID(), "SubmissionGraded");
    publisher.entered = new CountDownLatch(1);
    publisher.release = new CountDownLatch(1);
    try (var executor = java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor()) {
      var first = executor.submit(relay::relay);
      try {
        assertThat(publisher.entered.await(10, TimeUnit.SECONDS)).isTrue();
        assertThat(relay.relay()).isZero();
      } finally {
        publisher.release.countDown();
      }
      assertThat(first.get(15, TimeUnit.SECONDS)).isEqualTo(1);
    }
    assertThat(attempts(event)).isEqualTo(1);
    assertThat(pending()).isZero();
  }

  @Test
  void batchIsBoundedAtOneHundredAndAlreadyPublishedRowsAreExcluded() {
    publisher.fail.set(true);
    for (int i = 0; i < 101; i++) {
      seed(UUID.randomUUID(), "SubmissionGraded");
    }
    assertThat(relay.relay()).isEqualTo(100);
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM judge.outbox_events WHERE publish_attempts=1", Integer.class))
        .isEqualTo(100);
    jdbc.update(
        "UPDATE judge.outbox_events SET published_at=clock_timestamp() WHERE publish_attempts=1");
    assertThat(relay.relay()).isEqualTo(1);
  }

  @Test
  void uncommittedOutboxRowsCannotBeRelayed() {
    new TransactionTemplate(transactions)
        .executeWithoutResult(
            status -> {
              seed(UUID.randomUUID(), "SubmissionGraded");
              assertThat(relay.relay()).isZero();
              status.setRollbackOnly();
            });
    assertThat(pending()).isZero();
  }

  @Test
  void databaseFailureAfterBrokerAckRollsBackAndReplayDoesNotDoubleCount() throws Exception {
    UUID user = UUID.randomUUID();
    UUID event = seed(user, "SubmissionGraded");
    jdbc.execute(
        "CREATE FUNCTION judge.reject_relay() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN RAISE EXCEPTION 'relay write rejected'; END $$");
    jdbc.execute(
        "CREATE TRIGGER reject_relay BEFORE UPDATE ON judge.outbox_events FOR EACH ROW EXECUTE FUNCTION judge.reject_relay()");
    try {
      assertThatThrownBy(relay::relay).isInstanceOf(RuntimeException.class);
      awaitProcessed(event);
      assertThat(pending()).isEqualTo(1);
      assertThat(attempts(event)).isZero();
    } finally {
      jdbc.execute("DROP TRIGGER reject_relay ON judge.outbox_events");
      jdbc.execute("DROP FUNCTION judge.reject_relay()");
    }
    assertThat(relay.relay()).isEqualTo(1);
    UUID marker = seed(UUID.randomUUID(), "SubmissionGraded");
    relay.relay();
    awaitProcessed(marker);
    assertThat(total(user)).isEqualTo(1);
    assertThat(pending()).isZero();
  }

  private UUID seed(UUID user, String type) {
    jdbc.update(
        "INSERT INTO identity.users(id,email,display_name,role_id,status) SELECT ?,?,'Relay fixture',id,'ACTIVE' FROM identity.roles WHERE code='STUDENT' ON CONFLICT (id) DO NOTHING",
        user,
        user + "@example.test");
    UUID id = UUID.randomUUID();
    var payload =
        json.createObjectNode()
            .put("submissionId", UUID.randomUUID().toString())
            .put("userId", user.toString())
            .put("problemId", UUID.randomUUID().toString())
            .put("language", "PYTHON");
    if (type.equals("SubmissionGraded")) {
      payload
          .put("verdict", "ACCEPTED")
          .put("passedCount", 10)
          .put("totalCount", 10)
          .put("passedTestcaseRatio", 1)
          .put("submittedAt", "2026-11-03T16:59:00Z")
          .put("firstAttemptForProblem", true);
    }
    var envelope =
        json.createObjectNode()
            .put("eventId", id.toString())
            .put("eventType", type)
            .put("occurredAt", "2026-11-04T00:00:00Z")
            .put("schemaVersion", 1);
    envelope.set("payload", payload);
    jdbc.update(
        "INSERT INTO judge.outbox_events(id,aggregate_type,aggregate_id,event_type,payload) VALUES (?,'Submission',?,?,CAST(? AS jsonb))",
        id,
        UUID.randomUUID(),
        type,
        json.writeValueAsString(envelope));
    return id;
  }

  private int pending() {
    return jdbc.queryForObject(
        "SELECT count(*) FROM judge.outbox_events WHERE published_at IS NULL", Integer.class);
  }

  private int attempts(UUID id) {
    return jdbc.queryForObject(
        "SELECT publish_attempts FROM judge.outbox_events WHERE id=?", Integer.class, id);
  }

  private int total(UUID user) {
    return jdbc.queryForObject(
        "SELECT total_submissions FROM identity.user_submission_stats WHERE user_id=?",
        Integer.class,
        user);
  }

  private void awaitProcessed(UUID id) throws Exception {
    long deadline = System.nanoTime() + Duration.ofSeconds(15).toNanos();
    while (jdbc.queryForObject(
            "SELECT count(*) FROM identity.processed_domain_events WHERE event_id=?",
            Integer.class,
            id)
        == 0) {
      assertThat(System.nanoTime()).isLessThan(deadline);
      Thread.sleep(50);
    }
  }

  @TestConfiguration
  static class Transport {
    @Bean
    @Primary
    ControlledPublisher controlledPublisher(
        @Qualifier("rabbitJudgeDomainEventAdapter") JudgeDomainEventPort actual) {
      return new ControlledPublisher(actual);
    }
  }

  static class ControlledPublisher implements JudgeDomainEventPort {
    private final JudgeDomainEventPort actual;
    final AtomicBoolean fail = new AtomicBoolean();
    volatile CountDownLatch entered;
    volatile CountDownLatch release;

    ControlledPublisher(JudgeDomainEventPort actual) {
      this.actual = actual;
    }

    @Override
    public boolean publish(OutboxEvent event) {
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
      return !fail.get() && actual.publish(event);
    }
  }
}
