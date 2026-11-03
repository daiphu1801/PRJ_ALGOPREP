package com.algoprep.identity.infrastructure.messaging;

import com.algoprep.identity.domain.ports.out.ClassEventPort;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Publishes the envelope {@code { eventId, eventType, occurredAt, payload }} (DD api F1 section 2)
 * to topic exchange {@link ClassEventExchangeConfig#EXCHANGE} after the surrounding transaction
 * commits. A publish failure is logged and swallowed: no outbox, an accepted risk (BR-11, BR-22).
 * The body is written by hand because every value is a UUID or an ISO-8601 string.
 */
@Component
class AmqpClassEventAdapter implements ClassEventPort {
  private static final Logger LOG = LoggerFactory.getLogger(AmqpClassEventAdapter.class);
  private final ObjectProvider<RabbitTemplate> rabbit;

  AmqpClassEventAdapter(ObjectProvider<RabbitTemplate> rabbit) {
    this.rabbit = rabbit;
  }

  @Override
  public void classDeleted(UUID classId, Instant occurredAt) {
    publish("ClassDeleted", "class.deleted", occurredAt, "{\"classId\":\"" + classId + "\"}");
  }

  @Override
  public void studentRemoved(UUID classId, UUID studentId, Instant occurredAt) {
    publish(
        "StudentRemovedFromClass",
        "class.student-removed",
        occurredAt,
        "{\"classId\":\"" + classId + "\",\"studentId\":\"" + studentId + "\"}");
  }

  private void publish(String type, String routingKey, Instant occurredAt, String payload) {
    String body =
        "{\"eventId\":\""
            + UUID.randomUUID()
            + "\",\"eventType\":\""
            + type
            + "\",\"occurredAt\":\""
            + occurredAt
            + "\",\"payload\":"
            + payload
            + "}";
    if (TransactionSynchronizationManager.isSynchronizationActive()) {
      TransactionSynchronizationManager.registerSynchronization(
          new TransactionSynchronization() {
            @Override
            public void afterCommit() {
              send(type, routingKey, body);
            }
          });
    } else {
      send(type, routingKey, body);
    }
  }

  private void send(String type, String routingKey, String body) {
    try {
      Message message =
          MessageBuilder.withBody(body.getBytes(StandardCharsets.UTF_8))
              .setContentType(MessageProperties.CONTENT_TYPE_JSON)
              .setContentEncoding("UTF-8")
              .build();
      rabbit.getObject().send(ClassEventExchangeConfig.EXCHANGE, routingKey, message);
    } catch (RuntimeException e) {
      LOG.error("Could not publish {} to {}; the event is lost", type, routingKey, e);
    }
  }
}
