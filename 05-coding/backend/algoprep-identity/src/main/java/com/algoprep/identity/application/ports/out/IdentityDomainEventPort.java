package com.algoprep.identity.application.ports.out;

import com.algoprep.identity.domain.model.OutboxEvent;

public interface IdentityDomainEventPort {
  /** False means retry later: unconfirmed or unroutable. */
  boolean publish(OutboxEvent event);
}
