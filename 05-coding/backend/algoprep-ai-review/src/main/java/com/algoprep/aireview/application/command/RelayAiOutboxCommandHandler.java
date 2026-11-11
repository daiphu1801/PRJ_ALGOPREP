package com.algoprep.aireview.application.command;

import com.algoprep.aireview.application.ports.in.RelayAiOutboxUseCase;
import com.algoprep.aireview.application.ports.out.AiDomainEventPort;
import com.algoprep.aireview.domain.model.AiOutboxEvent;
import com.algoprep.aireview.domain.ports.out.AiOutboxRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * JOB-AI-01. Pending rows are locked with SKIP LOCKED, published with a broker confirm and marked
 * only on confirm; an unconfirmed row stays pending and is retried. At-least-once: the consumer
 * de-duplicates by event id.
 */
@Service
class RelayAiOutboxCommandHandler implements RelayAiOutboxUseCase {
  private static final Logger LOG = LoggerFactory.getLogger(RelayAiOutboxCommandHandler.class);
  // Same thresholds as the judge relay (DD jobs F5 section 1).
  private static final int WARN_ATTEMPTS = 3;
  private static final int ERROR_ATTEMPTS = 5;

  private final AiOutboxRepository repository;
  private final AiDomainEventPort publisher;
  private final int batchSize;

  RelayAiOutboxCommandHandler(
      AiOutboxRepository repository,
      AiDomainEventPort publisher,
      @Value("${ai.outbox.batch-size:100}") int batchSize) {
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
        warn(event);
      }
    }
    return pending.size();
  }

  private boolean publish(AiOutboxEvent event) {
    if (event.routingKey() == null) {
      LOG.error(
          "AI outbox row has an unknown event type, left unpublished: eventId={}, eventType={}",
          event.id(),
          event.eventType());
      return false;
    }
    try {
      return publisher.publish(event, AiEventEnvelope.of(event));
    } catch (RuntimeException unavailable) {
      // A transport or payload failure must not roll back attempt accounting nor expose the
      // payload.
      return false;
    }
  }

  private static void warn(AiOutboxEvent event) {
    long attempts = (long) event.attempts() + 1;
    if (attempts >= ERROR_ATTEMPTS) {
      LOG.error(
          "AI outbox delivery still pending: eventId={}, eventType={}, attempts={}",
          event.id(),
          event.eventType(),
          attempts);
    } else if (attempts >= WARN_ATTEMPTS) {
      LOG.warn(
          "AI outbox delivery pending: eventId={}, eventType={}, attempts={}",
          event.id(),
          event.eventType(),
          attempts);
    } else {
      LOG.info(
          "AI outbox delivery will be retried: eventId={}, eventType={}, attempts={}",
          event.id(),
          event.eventType(),
          attempts);
    }
  }
}
