package com.algoprep.judge.infrastructure.worker;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.algoprep.judge.domain.ports.out.WorkerJobRepository;
import com.algoprep.judge.domain.ports.out.WorkerLeasePort;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.MountableFile;

/** Real PG/Rabbit ownership, concurrency and drain; jobs use controlled completion barriers. */
@SpringBootTest(
    classes = com.algoprep.bootstrap.AlgoPrepApplication.class,
    properties = {
      "judge.worker.enabled=false",
      "spring.autoconfigure.exclude=org.springframework.boot.data.redis.autoconfigure.RedisAutoConfiguration",
      "spring.datasource.hikari.maximum-pool-size=10"
    })
@ActiveProfiles("test")
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class WorkerOperationsFlowTest {
  @Container @ServiceConnection
  static final PostgreSQLContainer POSTGRES =
      new PostgreSQLContainer("postgres:16.15-alpine")
          .withDatabaseName("algoprep")
          .withCopyFileToContainer(
              MountableFile.forHostPath("../../database/init/01-schemas.sql"),
              "/docker-entrypoint-initdb.d/01-schemas.sql");

  @Container
  static final GenericContainer<?> RABBIT =
      new GenericContainer<>("rabbitmq:4.2.9-management-alpine")
          .withEnv("RABBITMQ_DEFAULT_USER", "test-worker")
          .withEnv("RABBITMQ_DEFAULT_PASS", "test-worker-password")
          .withExposedPorts(5672)
          .waitingFor(Wait.forLogMessage(".*Server startup complete.*", 1));

  @DynamicPropertySource
  static void broker(DynamicPropertyRegistry properties) {
    properties.add("spring.rabbitmq.host", RABBIT::getHost);
    properties.add("spring.rabbitmq.port", () -> RABBIT.getMappedPort(5672));
    properties.add("spring.rabbitmq.username", () -> "test-worker");
    properties.add("spring.rabbitmq.password", () -> "test-worker-password");
  }

  @Autowired JdbcTemplate jdbc;
  @Autowired WorkerJobRepository jobs;
  @Autowired WorkerLeasePort leases;
  @Autowired ConnectionFactory connection;

  private final class Fixture implements AutoCloseable {
    final String queue = "judge.operations." + UUID.randomUUID();
    final SimpleMessageListenerContainer container = new SimpleMessageListenerContainer(connection);
    final WorkerConsumptionControl control = new WorkerConsumptionControl(jobs, leases, container);
    final CountDownLatch release = new CountDownLatch(1);
    final AtomicInteger started = new AtomicInteger();
    final AtomicInteger completed = new AtomicInteger();
    final AtomicInteger interrupted = new AtomicInteger();
    final SimpleMeterRegistry registry = new SimpleMeterRegistry();

    Fixture(int count) throws Exception {
      jdbc.update("DELETE FROM judge.submissions");
      jdbc.update(
          "UPDATE judge.cluster_settings SET queue_paused=false,autoscale_enabled=true,autoscale_job_threshold=1");
      declareAndPublish(count);
      container.setQueueNames(queue);
      container.setAcknowledgeMode(AcknowledgeMode.MANUAL);
      container.setPrefetchCount(1);
      container.setConcurrency("1");
      container.setAutoStartup(false);
      container.setShutdownTimeout(0);
      container.setForceCloseChannel(false);
      container.setMessageListener(new WorkerMessageListener(this::process));
      new WorkerConsumerMetrics(control).bindTo(registry);
      control.start();
    }

    private void process(UUID id) {
      try (var lease = leases.acquire("judge:sub:" + id).orElseThrow()) {
        started.incrementAndGet();
        if (!release.await(30, TimeUnit.SECONDS)) {
          throw new IllegalStateException("Completion barrier timed out");
        }
        lease.check();
        completed.incrementAndGet();
      } catch (InterruptedException failure) {
        interrupted.incrementAndGet();
        Thread.currentThread().interrupt();
        throw new IllegalStateException(failure);
      }
    }

    private void declareAndPublish(int count) throws Exception {
      try (var c = raw().newConnection();
          var channel = c.createChannel()) {
        channel.queueDeclare(queue, false, false, false, null);
        for (int i = 0; i < count; i++) {
          UUID id = UUID.randomUUID();
          jdbc.update(
              "INSERT INTO judge.submissions(id,user_id,problem_id,language,submission_model,source_code,source_input_method,status,testcase_set_version_at_grading) VALUES(?,?,?,'PYTHON','STANDARD_IO','fixture','EDITOR','PENDING',7)",
              id,
              UUID.randomUUID(),
              UUID.randomUUID());
          channel.basicPublish(
              "",
              queue,
              null,
              ("{\"submissionId\":\"" + id + "\"}").getBytes(StandardCharsets.UTF_8));
        }
      }
    }

    int consumers() {
      try (var c = raw().newConnection();
          var channel = c.createChannel()) {
        return channel.queueDeclarePassive(queue).getConsumerCount();
      } catch (Exception failure) {
        throw new IllegalStateException(failure);
      }
    }

    double metric(String suffix) {
      return registry.get("judge.grading.consumers." + suffix).gauge().value();
    }

    void scaleTo(int target) {
      for (int i = 1; i < target; i++) {
        control.scale();
        control.scale();
      }
      await().atMost(Duration.ofSeconds(20)).until(() -> started.get() >= target);
    }

    @Override
    public void close() {
      release.countDown();
      control.stop();
      container.destroy();
      registry.close();
      try (var c = raw().newConnection();
          var channel = c.createChannel()) {
        channel.queueDelete(queue);
      } catch (Exception failure) {
        throw new IllegalStateException(failure);
      }
    }
  }

  private static com.rabbitmq.client.ConnectionFactory raw() {
    var factory = new com.rabbitmq.client.ConnectionFactory();
    factory.setHost(RABBIT.getHost());
    factory.setPort(RABBIT.getMappedPort(5672));
    factory.setUsername("test-worker");
    factory.setPassword("test-worker-password");
    return factory;
  }

  @Test
  void eightJobsFitTenConnectionsAndPauseDrainsWithoutStartingTheNinth() throws Exception {
    try (var executor = Executors.newSingleThreadExecutor();
        var fixture = new Fixture(9)) {
      fixture.scaleTo(8);
      for (int i = 0; i < 4; i++) {
        fixture.control.scale();
      }
      assertThat(fixture.metric("target")).isEqualTo(8);
      assertThat(fixture.metric("active")).isEqualTo(8);
      jdbc.update("UPDATE judge.cluster_settings SET queue_paused=true");
      var stopped = executor.submit(fixture.control::gate);
      await().atMost(Duration.ofSeconds(10)).until(() -> fixture.consumers() == 0);
      assertThat(stopped.isDone()).isFalse();
      assertThat(fixture.started.get()).isEqualTo(8);
      org.junit.jupiter.api.Assertions.assertTimeoutPreemptively(
          Duration.ofSeconds(1), () -> assertThat(fixture.metric("target")).isEqualTo(8));
      fixture.release.countDown();
      stopped.get(10, TimeUnit.SECONDS);
      assertThat(fixture.completed.get()).isEqualTo(8);
      assertThat(fixture.interrupted.get()).isZero();
      assertThat(fixture.metric("active")).isZero();
      jdbc.update("UPDATE judge.cluster_settings SET queue_paused=false");
      fixture.control.gate();
      await().atMost(Duration.ofSeconds(10)).until(() -> fixture.completed.get() == 9);
      assertThat(fixture.control.ready()).isTrue();
    }
  }

  @Test
  void disablingAutoscaleDrainsBusyConsumersBeforeSettlingAtOne() throws Exception {
    try (var fixture = new Fixture(2)) {
      fixture.scaleTo(2);
      jdbc.update("UPDATE judge.cluster_settings SET autoscale_enabled=false");
      fixture.control.scale();
      assertThat(fixture.metric("target")).isEqualTo(1);
      assertThat(fixture.completed.get()).isZero();
      assertThat(fixture.interrupted.get()).isZero();
      fixture.release.countDown();
      await()
          .atMost(Duration.ofSeconds(15))
          .until(() -> fixture.completed.get() == 2 && fixture.metric("active") == 1);
      assertThat(fixture.interrupted.get()).isZero();
    }
  }

  @Test
  void losingTheRealLeaderSessionStopsConsumptionAndLetsAnotherWorkerCompete() throws Exception {
    try (var fixture = new Fixture(0)) {
      await().atMost(Duration.ofSeconds(10)).until(() -> fixture.consumers() == 1);
      var pids =
          jdbc.queryForList(
              "SELECT pid FROM pg_locks WHERE locktype='advisory' AND objid=(hashtext('F4_GRADING_WORKER_LEADER')::bigint & 4294967295)::oid AND objsubid=1",
              Integer.class);
      assertThat(pids).hasSize(1);
      assertThat(
              jdbc.queryForObject("SELECT pg_terminate_backend(?)", Boolean.class, pids.getFirst()))
          .isTrue();
      fixture.control.gate();
      assertThat(fixture.control.ready()).isFalse();
      assertThat(fixture.control.isRunning()).isFalse();
      org.assertj.core.api.Assertions.assertThatThrownBy(fixture.control::start)
          .isInstanceOf(IllegalStateException.class);
      await().atMost(Duration.ofSeconds(10)).until(() -> fixture.consumers() == 0);
      try (var next = leases.acquire("F4_GRADING_WORKER_LEADER").orElseThrow()) {
        next.check();
        fixture.control.gate();
        assertThat(fixture.consumers()).isZero();
      }
    }
  }
}
