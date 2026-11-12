package com.algoprep.identity.infrastructure.messaging;

import com.algoprep.identity.domain.ports.out.ClassEventPort;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** Stores class events in the business transaction; the relay publishes committed rows only. */
@Component
class AmqpClassEventAdapter implements ClassEventPort {
  private static final Logger LOG = LoggerFactory.getLogger(AmqpClassEventAdapter.class);
  private final ObjectProvider<JdbcClient> jdbc;
  private final ObjectProvider<RabbitTemplate> rabbit;

  AmqpClassEventAdapter(ObjectProvider<JdbcClient> jdbc, ObjectProvider<RabbitTemplate> rabbit) {
    this.jdbc = jdbc;
    this.rabbit = rabbit;
  }

  @Override
  @Transactional(propagation = Propagation.MANDATORY)
  public void classDeleted(UUID classId, Instant at) {
    append(classId, "ClassDeleted", "class.deleted", at, "{\"classId\":\"" + classId + "\"}");
  }

  /** No consumer yet: preserve the existing best-effort notification after commit. */
  @Override
  public void studentRemoved(UUID classId, UUID studentId, Instant at) {
    String body =
        "{\"eventId\":\""
            + UUID.randomUUID()
            + "\",\"eventType\":\"StudentRemovedFromClass\",\"occurredAt\":\""
            + at
            + "\",\"payload\":{\"classId\":\""
            + classId
            + "\",\"studentId\":\""
            + studentId
            + "\"}}";
    if (TransactionSynchronizationManager.isSynchronizationActive()) {
      TransactionSynchronizationManager.registerSynchronization(
          new TransactionSynchronization() {
            @Override
            public void afterCommit() {
              sendStudentRemoved(body);
            }
          });
    } else {
      sendStudentRemoved(body);
    }
  }

  private void sendStudentRemoved(String body) {
    try {
      rabbit
          .getObject()
          .send(
              ClassEventExchangeConfig.EXCHANGE,
              "class.student-removed",
              MessageBuilder.withBody(body.getBytes(StandardCharsets.UTF_8))
                  .setContentType(MessageProperties.CONTENT_TYPE_JSON)
                  .setContentEncoding("UTF-8")
                  .build());
    } catch (RuntimeException unavailable) {
      LOG.error("StudentRemovedFromClass notification unavailable; no consumer is configured");
    }
  }

  private void append(UUID classId, String type, String key, Instant at, String payload) {
    UUID id = UUID.randomUUID();
    String envelope =
        "{\"eventId\":\""
            + id
            + "\",\"eventType\":\""
            + type
            + "\",\"occurredAt\":\""
            + at
            + "\",\"payload\":"
            + payload
            + "}";
    jdbc.getObject()
        .sql(
            """
        INSERT INTO identity.outbox_events
          (id, aggregate_type, aggregate_id, event_type, routing_key, payload, created_at)
        VALUES (:id, 'CLASS', :classId, :type, :key, CAST(:body AS jsonb), :at)
        """)
        .param("id", id)
        .param("classId", classId)
        .param("type", type)
        .param("key", key)
        .param("body", envelope)
        .param("at", Timestamp.from(at))
        .update();
  }
}
