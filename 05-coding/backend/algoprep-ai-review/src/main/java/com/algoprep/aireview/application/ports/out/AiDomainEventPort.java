package com.algoprep.aireview.application.ports.out;

import com.algoprep.aireview.domain.model.AiOutboxEvent;

public interface AiDomainEventPort {
  /** False means retry later: unconfirmed or unroutable. {@code envelope} is the JSON body. */
  boolean publish(AiOutboxEvent event, String envelope);
}
