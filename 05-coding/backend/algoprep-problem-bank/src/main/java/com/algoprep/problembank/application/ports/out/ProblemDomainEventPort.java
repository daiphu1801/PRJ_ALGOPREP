package com.algoprep.problembank.application.ports.out;

import com.algoprep.problembank.domain.model.OutboxEvent;

public interface ProblemDomainEventPort {
  /** False means retry later: unconfirmed or unroutable. */
  boolean publish(OutboxEvent event);
}
