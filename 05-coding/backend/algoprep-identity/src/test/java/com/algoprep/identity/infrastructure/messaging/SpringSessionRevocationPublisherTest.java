package com.algoprep.identity.infrastructure.messaging;

import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.algoprep.identity.domain.model.auth.SessionRevoked;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.support.TransactionSynchronizationManager;

class SpringSessionRevocationPublisherTest {
  @Test
  void transactionalPublicationWaitsForCommitAndRollbackDoesNotPublish() {
    var events = mock(ApplicationEventPublisher.class);
    var publisher = new SpringSessionRevocationPublisher(events);
    var event = new SessionRevoked(UUID.randomUUID());
    TransactionSynchronizationManager.initSynchronization();
    try {
      publisher.publish(event);
      verifyNoInteractions(events);
      TransactionSynchronizationManager.getSynchronizations().getFirst().afterCommit();
      verify(events).publishEvent(event);
    } finally {
      TransactionSynchronizationManager.clearSynchronization();
    }
    clearInvocations(events);
    TransactionSynchronizationManager.initSynchronization();
    try {
      publisher.publish(event);
      TransactionSynchronizationManager.getSynchronizations().getFirst().afterCompletion(1);
      verifyNoInteractions(events);
    } finally {
      TransactionSynchronizationManager.clearSynchronization();
    }
    publisher.publish(event);
    verify(events).publishEvent(event);
  }
}
