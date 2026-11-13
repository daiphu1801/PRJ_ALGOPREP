package com.algoprep.bootstrap.judge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.algoprep.judge.application.ports.in.GetQueueMonitorBatchUseCase;
import com.algoprep.judge.domain.ports.out.QueueMonitorRepository;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.MountableFile;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
      "spring.autoconfigure.exclude=org.springframework.boot.data.redis.autoconfigure.RedisAutoConfiguration,org.springframework.boot.amqp.autoconfigure.RabbitAutoConfiguration",
      "judge.admin-stream.enabled=false",
      "spring.datasource.hikari.maximum-pool-size=6"
    })
@ActiveProfiles("test")
@Testcontainers
@Import(QueueStreamBrokerBarrier.class)
class QueueMonitorStreamFlowTest extends QueueStreamTestSupport {
  @Container @ServiceConnection
  static final PostgreSQLContainer POSTGRES =
      new PostgreSQLContainer("postgres:16.15-alpine")
          .withDatabaseName("algoprep")
          .withCopyFileToContainer(
              MountableFile.forHostPath("../../database/init/01-schemas.sql"),
              "/docker-entrypoint-initdb.d/01-schemas.sql");

  @MockitoSpyBean GetQueueMonitorBatchUseCase assembly;
  @MockitoSpyBean QueueMonitorRepository queue;

  @Test
  void twoAdminSessionsReceiveOneIdenticalRealAggregateWithoutPrivateData() throws Exception {
    seed();
    try (var first = QueueStreamWireClient.connect(port, admin);
        var second = QueueStreamWireClient.connect(port, account(UUID.randomUUID(), "ADMIN"))) {
      first.subscribe("first");
      second.subscribe("second");
      await(audience::hasSubscribers);
      clearInvocations(assembly);
      ticks.broadcast();
      var a = body(first.next());
      assertThat(body(second.next())).isEqualTo(a);
      verify(assembly).read();
      assertThat(a.properties().stream().map(Map.Entry::getKey))
          .containsExactlyInAnyOrder(
              "type", "generatedAt", "snapshot", "jobCounts", "infraSummary");
      assertThat(a.path("type").asText()).isEqualTo("QUEUE_BATCH");
      assertThat(a.path("snapshot").path("generatedAt").asText())
          .isEqualTo(a.path("generatedAt").asText());
      assertThat(a.path("snapshot").path("pendingCount").asLong()).isEqualTo(2);
      assertThat(a.path("snapshot").path("avgWaitMillis").asLong()).isEqualTo(2000);
      assertThat(a.path("jobCounts").path("waiting").asLong()).isEqualTo(2);
      assertThat(a.path("jobCounts").path("failed").asLong()).isEqualTo(1);
      assertThat(a.path("jobCounts").path("all").asLong()).isEqualTo(5);
      assertThat(a.path("infraSummary").properties().stream().map(Map.Entry::getKey))
          .containsExactlyInAnyOrder("warnCount", "errorCount");
      assertThat(a.path("infraSummary").path("warnCount").asLong()).isEqualTo(1);
      assertThat(a.path("infraSummary").path("errorCount").asLong()).isEqualTo(2);
      assertThat(a.toString())
          .doesNotContain(
              "items",
              "windowHours",
              "STREAM_PRIVATE",
              "sourceCode",
              "testcase",
              "workerId",
              operator.toString());
    }
  }

  @Test
  void unsubscribeAndDisconnectStopReadsAndReconnectGetsFreshSettings() throws Exception {
    try (var first = QueueStreamWireClient.connect(port, admin);
        var second = QueueStreamWireClient.connect(port, admin)) {
      first.subscribe("one");
      second.subscribe("two");
      first.unsubscribe("one");
      ticks.broadcast();
      body(second.next());
      first.noMessage();
      second.unsubscribe("two");
      await(() -> !audience.hasSubscribers());
      clearInvocations(assembly);
      ticks.broadcast();
      verifyNoInteractions(assembly);
      second.subscribe("again");
      jdbc.update("UPDATE judge.cluster_settings SET queue_paused=true,latency_cap_seconds=42");
      ticks.broadcast();
      var state = body(second.next()).path("snapshot");
      assertThat(state.path("status").asText()).isEqualTo("PAUSED");
      assertThat(state.path("latencyCapSeconds").asInt()).isEqualTo(42);
    }
    await(() -> !audience.hasSubscribers());
    clearInvocations(assembly);
    ticks.broadcast();
    verifyNoInteractions(assembly);
  }

  @Test
  void studentAndRevokedReadCellCannotJoinOrCauseDatabasePolling() throws Exception {
    try (var student = QueueStreamWireClient.connect(port, account(UUID.randomUUID(), "STUDENT"))) {
      student.requestSubscribe("student");
      student.denied();
    }
    try (var operatorWire = QueueStreamWireClient.connect(port, admin)) {
      setRead(false);
      try {
        operatorWire.requestSubscribe("revoked");
        operatorWire.denied();
      } finally {
        setRead(true);
      }
    }
    await(() -> !audience.hasSubscribers());
    clearInvocations(assembly);
    ticks.broadcast();
    verifyNoInteractions(assembly);
  }

  @Test
  void readFailureSendsNoFalseBatchAndTheFollowingTickRecovers() throws Exception {
    try (var wire = QueueStreamWireClient.connect(port, admin)) {
      wire.subscribe("fault");
      doThrow(new DataAccessResourceFailureException("private SQL source_code"))
          .when(assembly)
          .read();
      ticks.broadcast();
      wire.noMessage();
      reset(assembly);
      ticks.broadcast();
      assertThat(body(wire.next()).path("jobCounts").path("all").asLong()).isZero();
    }
  }

  @Test
  void concurrentCommitCannotMixSnapshotCountsSettingsOrInfraSummary() {
    seed();
    var once = new AtomicBoolean();
    doAnswer(
            inv -> {
              var facts = inv.callRealMethod();
              if (once.compareAndSet(false, true)) {
                var writer = new TransactionTemplate(transactions);
                writer.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
                writer.executeWithoutResult(
                    tx -> {
                      jdbc.update("UPDATE judge.cluster_settings SET queue_paused=true");
                      jdbc.update(
                          "UPDATE judge.submissions SET status='ACCEPTED',finished_at=transaction_timestamp() WHERE status='PENDING'");
                      jdbc.update(
                          "UPDATE judge.outbox_events SET publish_attempts=5 WHERE publish_attempts=3");
                    });
              }
              return facts;
            })
        .when(queue)
        .snapshot();
    var before = batches.read();
    assertThat(before.snapshot().pendingCount()).isEqualTo(2);
    assertThat(before.jobCounts().waiting()).isEqualTo(2);
    assertThat(before.snapshot().status()).isEqualTo("NORMAL");
    assertThat(before.infraSummary().warnCount()).isEqualTo(1);
    var after = batches.read();
    assertThat(after.snapshot().status()).isEqualTo("PAUSED");
    assertThat(after.jobCounts().waiting()).isZero();
    assertThat(after.infraSummary().warnCount()).isZero();
    assertThat(after.infraSummary().errorCount()).isEqualTo(3);
  }
}
