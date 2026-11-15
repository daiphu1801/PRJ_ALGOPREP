package com.algoprep.problembank.application.command;

import com.algoprep.problembank.application.ports.in.RelayProblemOutboxUseCase;
import com.algoprep.problembank.application.ports.out.ProblemDomainEventPort;
import com.algoprep.problembank.domain.model.OutboxEvent;
import com.algoprep.problembank.domain.ports.out.ProblemOutboxRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

@Service
class RelayProblemOutboxCommandHandler implements RelayProblemOutboxUseCase {
  private static final Logger LOG = LoggerFactory.getLogger(RelayProblemOutboxCommandHandler.class);
  private static final JsonMapper JSON = JsonMapper.builder().build();
  private final ProblemOutboxRepository repository;
  private final ProblemDomainEventPort publisher;
  private final int batchSize;

  RelayProblemOutboxCommandHandler(
      ProblemOutboxRepository repository,
      ProblemDomainEventPort publisher,
      @Value("${problem.outbox.batch-size:100}") int batchSize) {
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
            "Problem outbox delivery pending: eventId={}, eventType={}, attempts={}",
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
          "Problem outbox row unusable, left unpublished: eventId={}, eventType={}",
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
