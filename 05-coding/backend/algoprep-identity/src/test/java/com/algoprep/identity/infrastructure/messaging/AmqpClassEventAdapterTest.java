package com.algoprep.identity.infrastructure.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** Envelope shape, routing keys, after-commit timing and the swallowed failure (BR-11, BR-22). */
class AmqpClassEventAdapterTest {
  private final RabbitTemplate template = mock(RabbitTemplate.class);

  @SuppressWarnings("unchecked")
  private final ObjectProvider<RabbitTemplate> provider = mock(ObjectProvider.class);

  private final AmqpClassEventAdapter adapter = new AmqpClassEventAdapter(provider);
  private final UUID classId = UUID.randomUUID();
  private final UUID studentId = UUID.randomUUID();
  private final Instant at = Instant.parse("2026-10-30T08:00:00.123456Z");

  AmqpClassEventAdapterTest() {
    when(provider.getObject()).thenReturn(template);
  }

  @AfterEach
  void clean() {
    if (TransactionSynchronizationManager.isSynchronizationActive()) {
      TransactionSynchronizationManager.clearSynchronization();
    }
  }

  private String sent(String routingKey) {
    ArgumentCaptor<Message> message = ArgumentCaptor.forClass(Message.class);
    verify(template).send(eq("algoprep.identity.events"), eq(routingKey), message.capture());
    return new String(message.getValue().getBody(), StandardCharsets.UTF_8);
  }

  @Test
  void withoutATransactionTheEventGoesOutAtOnce() {
    adapter.studentRemoved(classId, studentId, at);

    assertThat(sent("class.student-removed"))
        .contains("\"eventType\":\"StudentRemovedFromClass\"")
        .contains("\"occurredAt\":\"2026-10-30T08:00:00.123456Z\"")
        .contains(
            "\"payload\":{\"classId\":\"" + classId + "\",\"studentId\":\"" + studentId + "\"}")
        .containsPattern("\"eventId\":\"[0-9a-f-]{36}\"");
  }

  @Test
  void classDeletedCarriesOnlyTheClassId() {
    adapter.classDeleted(classId, at);

    assertThat(sent("class.deleted"))
        .contains("\"eventType\":\"ClassDeleted\"")
        .contains("\"payload\":{\"classId\":\"" + classId + "\"}");
  }

  @Test
  void insideATransactionNothingIsSentUntilCommit() {
    TransactionSynchronizationManager.initSynchronization();

    adapter.classDeleted(classId, at);

    verify(template, never()).send(any(String.class), any(String.class), any(Message.class));
    for (TransactionSynchronization s : TransactionSynchronizationManager.getSynchronizations()) {
      s.afterCommit();
    }
    sent("class.deleted");
  }

  @Test
  void aBrokerFailureIsSwallowed() {
    doThrow(new AmqpException("broker down"))
        .when(template)
        .send(any(String.class), any(String.class), any(Message.class));

    assertThatCode(() -> adapter.classDeleted(classId, at)).doesNotThrowAnyException();
  }
}
