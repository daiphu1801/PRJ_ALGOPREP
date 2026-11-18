package com.algoprep.identity.infrastructure.messaging;

import com.algoprep.identity.application.ports.out.SessionRevocationPublisher;
import com.algoprep.identity.domain.model.auth.SessionRevoked;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
class SpringSessionRevocationPublisher implements SessionRevocationPublisher {
  private final ApplicationEventPublisher events;

  SpringSessionRevocationPublisher(ApplicationEventPublisher events) {
    this.events = events;
  }

  @Override
  public void publish(SessionRevoked event) {
    if (TransactionSynchronizationManager.isSynchronizationActive()) {
      TransactionSynchronizationManager.registerSynchronization(
          new TransactionSynchronization() {
            @Override
            public void afterCommit() {
              events.publishEvent(event);
            }
          });
    } else {
      events.publishEvent(event);
    }
  }
}
