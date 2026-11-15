package com.algoprep.bootstrap.judge;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.MountableFile;

/**
 * Post-commit revocation tested against real WebSocket messages and live PostgreSQL authority data.
 */
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
class QueueMonitorRevocationFlowTest extends QueueStreamTestSupport {
  @Container @ServiceConnection
  static final PostgreSQLContainer POSTGRES =
      new PostgreSQLContainer("postgres:16.15-alpine")
          .withDatabaseName("algoprep")
          .withCopyFileToContainer(
              MountableFile.forHostPath("../../database/init/01-schemas.sql"),
              "/docker-entrypoint-initdb.d/01-schemas.sql");

  @Test
  void lockedAccountStopsReceivingNewBatchesWhileAnotherAdminContinues() throws Exception {
    try (var revoked = QueueStreamWireClient.connect(port, admin);
        var allowed = QueueStreamWireClient.connect(port, account(UUID.randomUUID(), "ADMIN"))) {
      revoked.subscribe("lock-old");
      allowed.subscribe("lock-control");
      ticks.broadcast();
      body(revoked.next());
      body(allowed.next());
      jdbc.update(
          "UPDATE identity.users SET status='DEACTIVATED',deactivation_source='ADMIN',deactivated_at=now() WHERE id=?",
          operator);
      ticks.broadcast();
      assertThat(body(allowed.next()).path("type").asText()).isEqualTo("QUEUE_BATCH");
      revoked.noMessage();
      allowed.unsubscribe("lock-control");
      await(() -> !audience.hasSubscribers());
    }
  }

  @Test
  void permissionRevocationClosesExistingSubscriptionBeforeNewBatch() throws Exception {
    try (var revoked = QueueStreamWireClient.connect(port, admin)) {
      revoked.subscribe("cell-old");
      ticks.broadcast();
      body(revoked.next());
      setRead(false);
      try {
        ticks.broadcast();
        await(() -> !audience.hasSubscribers());
        assertThat(audience.hasSubscribers()).isFalse();
        revoked.noMessage();
      } finally {
        setRead(true);
      }
    }
  }

  @Test
  void roleChangeStopsMessagesToThePreviouslyAuthorizedSession() throws Exception {
    try (var revoked = QueueStreamWireClient.connect(port, admin)) {
      revoked.subscribe("role-old");
      ticks.broadcast();
      body(revoked.next());
      jdbc.update(
          "UPDATE identity.users SET role_id=(SELECT id FROM identity.roles WHERE code='INSTRUCTOR') WHERE id=?",
          operator);
      ticks.broadcast();
      await(() -> !audience.hasSubscribers());
      assertThat(audience.hasSubscribers()).isFalse();
      revoked.noMessage();
    }
  }
}
