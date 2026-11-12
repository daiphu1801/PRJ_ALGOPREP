package com.algoprep.identity.application.ports.in;

public interface RelayIdentityOutboxUseCase {
  /** JOB-ID-OUTBOX-01. Returns the number of rows attempted. */
  int relay();
}
