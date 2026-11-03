package com.algoprep.bootstrap.judge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.algoprep.judge.application.ports.in.GetQueueMonitorBatchUseCase;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.MountableFile;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
      "spring.autoconfigure.exclude=org.springframework.boot.data.redis.autoconfigure.RedisAutoConfiguration,org.springframework.boot.amqp.autoconfigure.RabbitAutoConfiguration",
      "spring.datasource.hikari.maximum-pool-size=6"
    })
@ActiveProfiles("test")
@Testcontainers
@Import(QueueStreamBrokerBarrier.class)
class QueueMonitorCadenceFlowTest extends QueueStreamTestSupport {
  @Container @ServiceConnection
  static final PostgreSQLContainer POSTGRES =
      new PostgreSQLContainer("postgres:16.15-alpine")
          .withDatabaseName("algoprep")
          .withCopyFileToContainer(
              MountableFile.forHostPath("../../database/init/01-schemas.sql"),
              "/docker-entrypoint-initdb.d/01-schemas.sql");

  @MockitoSpyBean GetQueueMonitorBatchUseCase assembly;

  @Test
  void defaultTimerBroadcastsEveryFiveSecondsAndBecomesIdleAfterDisconnect() throws Exception {
    seed();
    clearInvocations(assembly);
    try (var first = QueueStreamWireClient.connect(port, admin);
        var second = QueueStreamWireClient.connect(port, admin)) {
      first.subscribe("timer-one");
      second.subscribe("timer-two");
      var a = body(first.next());
      assertThat(body(second.next())).isEqualTo(a);
      jdbc.update("UPDATE judge.cluster_settings SET queue_paused=true");
      var b = body(first.next());
      assertThat(body(second.next())).isEqualTo(b);
      assertThat(Duration.between(generated(a), generated(b)).toMillis()).isBetween(4500L, 8000L);
      assertThat(b.path("snapshot").path("status").asText()).isEqualTo("PAUSED");
      verify(assembly, times(2)).read();
    }
    await(() -> !audience.hasSubscribers());
    clearInvocations(assembly);
    Thread.sleep(5500);
    verifyNoInteractions(assembly);
  }
}
