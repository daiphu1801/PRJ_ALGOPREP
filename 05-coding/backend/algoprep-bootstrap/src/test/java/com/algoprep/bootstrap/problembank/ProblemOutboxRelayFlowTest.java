package com.algoprep.bootstrap.problembank;

import static org.assertj.core.api.Assertions.assertThat;

import com.algoprep.problembank.application.ports.in.CleanProblemOutboxUseCase;
import com.algoprep.problembank.application.ports.in.RelayProblemOutboxUseCase;
import com.algoprep.problembank.application.ports.out.ProblemDomainEventPort;
import com.algoprep.problembank.domain.model.OutboxEvent;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
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
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.MountableFile;

/** JOB-PB-01 and JOB-PB-02 against real PostgreSQL and RabbitMQ (AC-89, AC-90, AC-94). */
@SpringBootTest(properties = {"spring.autoconfigure.exclude=", "problem.outbox.enabled=false"})
@ActiveProfiles("test")
@Testcontainers
@Import(ProblemOutboxRelayFlowTest.Transport.class)
class ProblemOutboxRelayFlowTest {
  private static final String QUEUE = "problem-outbox-flow-test";

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
  @Autowired RelayProblemOutboxUseCase relay;
  @Autowired CleanProblemOutboxUseCase cleanup;
  @Autowired ControlledPublisher publisher;
  @Autowired RabbitTemplate rabbit;

  @BeforeEach
  void reset() {
    jdbc.update("DELETE FROM problem.outbox_events");
    publisher.entered = null;
    publisher.release = null;
    rabbit.execute(
        channel -> {
          channel.exchangeDeclare("algoprep.domain-events", "topic", true);
          channel.queueDeclare(QUEUE, false, false, true, null);
          channel.queuePurge(QUEUE);
          channel.queueBind(QUEUE, "algoprep.domain-events", "problem.problem.*");
          return null;
        });
  }

  @Test
  void publishedRowIsMarkedAfterConfirmAndKeepsItsEventIdentity() {
    UUID id = seed("problem.problem.published", "{\"eventId\":\"x\"}");
    assertThat(relay.relay()).isEqualTo(1);
    assertThat(pending()).isZero();
    assertThat(attempts(id)).isEqualTo(1);
    var message = rabbit.receive(QUEUE, 5000);
    assertThat(message).isNotNull();
    assertThat(message.getMessageProperties().getMessageId()).isEqualTo(id.toString());
    assertThat(new String(message.getBody(), StandardCharsets.UTF_8)).contains("\"eventId\"");
    assertThat(relay.relay()).isZero();
  }

  @Test
  void unroutableKeyStaysPendingThenDeliversOnceABindingExists() {
    UUID id = seed("problem.class.assigned", "{}");
    assertThat(relay.relay()).isEqualTo(1);
    assertThat(pending()).isEqualTo(1);
    assertThat(attempts(id)).isEqualTo(1);
    rabbit.execute(
        channel -> {
          channel.queueBind(QUEUE, "algoprep.domain-events", "problem.class.assigned");
          return null;
        });
    assertThat(relay.relay()).isEqualTo(1);
    assertThat(pending()).isZero();
    assertThat(attempts(id)).isEqualTo(2);
    assertThat(rabbit.receive(QUEUE, 5000)).isNotNull();
  }

  @Test
  void rowWithBlankRoutingKeyIsKeptUnpublishedAndCounted() {
    UUID id = seed(" ", "{}");
    assertThat(relay.relay()).isEqualTo(1);
    assertThat(pending()).isEqualTo(1);
    assertThat(attempts(id)).isEqualTo(1);
  }

  @Test
  void secondRelaySkipsRowsLockedByTheFirst() throws Exception {
    UUID id = seed("problem.problem.published", "{}");
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
  void batchIsBoundedAtOneHundred() {
    for (int i = 0; i < 101; i++) {
      seed("problem.problem.published", "{}");
    }
    assertThat(relay.relay()).isEqualTo(100);
    assertThat(pending()).isEqualTo(1);
  }

  @Test
  void cleanupRemovesOnlyPublishedRowsPastRetention() {
    UUID oldPublished = seed("problem.problem.published", "{}");
    UUID freshPublished = seed("problem.problem.published", "{}");
    UUID oldPending = seed("problem.problem.published", "{}");
    jdbc.update(
        "UPDATE problem.outbox_events SET published_at = now() - interval '91 days' WHERE id=?",
        oldPublished);
    jdbc.update(
        "UPDATE problem.outbox_events SET published_at = now() - interval '89 days' WHERE id=?",
        freshPublished);
    jdbc.update(
        "UPDATE problem.outbox_events SET created_at = now() - interval '400 days' WHERE id=?",
        oldPending);
    assertThat(cleanup.clean()).isEqualTo(1);
    assertThat(exists(oldPublished)).isFalse();
    assertThat(exists(freshPublished)).isTrue();
    assertThat(exists(oldPending)).isTrue();
  }

  @Test
  void cleanupLoopsOverChunksOfOneThousand() {
    jdbc.update(
        """
        INSERT INTO problem.outbox_events(id,aggregate_type,aggregate_id,event_type,routing_key,payload,published_at)
        SELECT gen_random_uuid(),'Problem',gen_random_uuid(),'ProblemPublished','problem.problem.published','{}'::jsonb,
               now() - interval '100 days'
        FROM generate_series(1, 2500)
        """);
    assertThat(cleanup.clean()).isEqualTo(2500);
  }

  private UUID seed(String routingKey, String payload) {
    UUID id = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO problem.outbox_events(id,aggregate_type,aggregate_id,event_type,routing_key,payload) VALUES (?,'Problem',?,'ProblemPublished',?,CAST(? AS jsonb))",
        id,
        UUID.randomUUID(),
        routingKey,
        payload);
    return id;
  }

  private int pending() {
    return jdbc.queryForObject(
        "SELECT count(*) FROM problem.outbox_events WHERE published_at IS NULL", Integer.class);
  }

  private int attempts(UUID id) {
    return jdbc.queryForObject(
        "SELECT publish_attempts FROM problem.outbox_events WHERE id=?", Integer.class, id);
  }

  private boolean exists(UUID id) {
    return jdbc.queryForObject(
            "SELECT count(*) FROM problem.outbox_events WHERE id=?", Integer.class, id)
        > 0;
  }

  @TestConfiguration
  static class Transport {
    @Bean
    @Primary
    ControlledPublisher controlledPublisher(
        @Qualifier("rabbitProblemDomainEventAdapter") ProblemDomainEventPort actual) {
      return new ControlledPublisher(actual);
    }
  }

  static class ControlledPublisher implements ProblemDomainEventPort {
    private final ProblemDomainEventPort actual;
    volatile CountDownLatch entered;
    volatile CountDownLatch release;

    ControlledPublisher(ProblemDomainEventPort actual) {
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
      return actual.publish(event);
    }
  }
}
