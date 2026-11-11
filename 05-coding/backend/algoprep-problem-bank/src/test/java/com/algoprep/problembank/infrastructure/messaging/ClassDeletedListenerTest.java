package com.algoprep.problembank.infrastructure.messaging;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.algoprep.problembank.application.ports.in.ApplyClassDeletedUseCase;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;

class ClassDeletedListenerTest {
  private final ApplyClassDeletedUseCase useCase = mock(ApplyClassDeletedUseCase.class);
  private final ClassDeletedListener listener = new ClassDeletedListener(useCase);

  private byte[] message(UUID id) {
    return ("{\"eventType\":\"ClassDeleted\",\"eventId\":\""
            + UUID.randomUUID()
            + "\",\"occurredAt\":\"2026-11-10T10:00:00Z\",\"payload\":{\"classId\":\""
            + id
            + "\"}}")
        .getBytes(StandardCharsets.UTF_8);
  }

  @Test
  void validDeletionReachesTheTransactionalUseCase() {
    UUID id = UUID.randomUUID();
    listener.onMessage(message(id));
    verify(useCase).apply(id);
  }

  @Test
  void malformedEnvelopeNeverTouchesAssignments() {
    assertThatThrownBy(() -> listener.onMessage("{}".getBytes(StandardCharsets.UTF_8)))
        .isInstanceOf(AmqpRejectAndDontRequeueException.class);
    verifyNoInteractions(useCase);
  }

  @Test
  void persistenceFailureEscapesForBrokerRedelivery() {
    UUID id = UUID.randomUUID();
    var failure = new IllegalStateException("database down");
    when(useCase.apply(id)).thenThrow(failure);
    assertThatThrownBy(() -> listener.onMessage(message(id))).isSameAs(failure);
  }
}
