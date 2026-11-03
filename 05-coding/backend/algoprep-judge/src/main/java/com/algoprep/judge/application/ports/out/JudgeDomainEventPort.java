package com.algoprep.judge.application.ports.out;

import com.algoprep.judge.domain.model.OutboxEvent;

public interface JudgeDomainEventPort {
  /** False means retry later, including uncertain delivery and unroutable messages. */
  boolean publish(OutboxEvent event);
}
