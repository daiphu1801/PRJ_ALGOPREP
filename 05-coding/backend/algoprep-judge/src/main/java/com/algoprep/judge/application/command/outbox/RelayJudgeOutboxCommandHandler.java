package com.algoprep.judge.application.command;

import com.algoprep.judge.application.ports.in.RelayJudgeOutboxUseCase;
import com.algoprep.judge.application.ports.out.JudgeDomainEventPort;
import com.algoprep.judge.domain.model.OutboxEvent;
import com.algoprep.judge.domain.ports.out.JudgeOutboxRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
class RelayJudgeOutboxCommandHandler implements RelayJudgeOutboxUseCase {
  private static final Logger LOG = LoggerFactory.getLogger(RelayJudgeOutboxCommandHandler.class);
  private final JudgeOutboxRepository repository;
  private final JudgeDomainEventPort publisher;
  private final int warnAttempts;
  private final int errorAttempts;

  RelayJudgeOutboxCommandHandler(
      JudgeOutboxRepository repository,
      JudgeDomainEventPort publisher,
      @Value("${judge.infra.outbox-warn-attempts:3}") int warnAttempts,
      @Value("${judge.infra.outbox-error-attempts:5}") int errorAttempts) {
    this.repository = repository;
    this.publisher = publisher;
    this.warnAttempts = warnAttempts;
    this.errorAttempts = errorAttempts;
  }

  @Override
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public int relay() {
    var pending = repository.lockPending();
    for (var event : pending) {
      boolean confirmed = publish(event);
      repository.attempted(event.id(), confirmed);
      if (!confirmed) {
        warn(event);
      }
    }
    return pending.size();
  }

  private boolean publish(OutboxEvent event) {
    try {
      return publisher.publish(event);
    } catch (RuntimeException unavailable) {
      // A transport failure must not roll back attempt accounting or expose its payload.
      return false;
    }
  }

  private void warn(OutboxEvent event) {
    long attempts = (long) event.attempts() + 1;
    if (attempts >= errorAttempts) {
      LOG.error("Judge outbox delivery pending: event={}, attempts={}", event.id(), attempts);
    } else if (attempts >= warnAttempts) {
      LOG.warn("Judge outbox delivery pending: event={}, attempts={}", event.id(), attempts);
    }
  }
}
