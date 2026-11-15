package com.algoprep.identity.application.ports.out;

import com.algoprep.identity.domain.model.auth.SessionRevoked;

/** Publishes only after the revocation transaction has committed. */
public interface SessionRevocationPublisher {
  void publish(SessionRevoked event);
}
