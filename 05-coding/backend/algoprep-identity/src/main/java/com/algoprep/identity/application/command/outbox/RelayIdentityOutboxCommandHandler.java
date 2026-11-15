package com.algoprep.identity.application.command;

import com.algoprep.identity.application.ports.in.RelayIdentityOutboxUseCase;
import com.algoprep.identity.application.ports.out.IdentityDomainEventPort;
import com.algoprep.identity.domain.model.OutboxEvent;
import com.algoprep.identity.domain.ports.out.IdentityOutboxRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

@Service
class RelayIdentityOutboxCommandHandler implements RelayIdentityOutboxUseCase {
  private static final Logger LOG =
      LoggerFactory.getLogger(RelayIdentityOutboxCommandHandler.class);
  private static final JsonMapper JSON = JsonMapper.builder().build();
  private final IdentityOutboxRepository repository;
  private final IdentityDomainEventPort publisher;
  private final int batchSize;

  RelayIdentityOutboxCommandHandler(
      IdentityOutboxRepository repository,
      IdentityDomainEventPort publisher,
      @Value("${identity.outbox.batch-size:100}") int batchSize) {
    this.repository = repository;
    this.publisher = publisher;
    this.batchSize = batchSize;
  }

  @Override
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public int relay() {
    var pending = repository.lockPending(batchSize);
    for (var event : pending) {
      boolean confirmed = publish(event);
      repository.attempted(event.id(), confirmed);
      if (!confirmed) {
        LOG.warn(
            "Identity outbox delivery pending: eventId={}, eventType={}, attempts={}",
            event.id(),
            event.eventType(),
            (long) event.attempts() + 1);
      }
    }
    return pending.size();
  }

  private boolean publish(OutboxEvent event) {
    if (event.routingKey() == null || event.routingKey().isBlank() || !validJson(event)) {
      LOG.error(
          "Identity outbox row unusable, left unpublished: eventId={}, eventType={}",
          event.id(),
          event.eventType());
      return false;
    }
    try {
      return publisher.publish(event);
    } catch (RuntimeException unavailable) {
      // A transport failure must not roll back attempt accounting or expose its payload.
      return false;
    }
  }

  private static boolean validJson(OutboxEvent event) {
    try {
      JSON.readTree(event.envelope());
      return true;
    } catch (RuntimeException invalid) {
      return false;
    }
  }
}
