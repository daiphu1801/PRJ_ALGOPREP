package com.algoprep.identity.application.ports.in.outbox;

public interface CleanIdentityOutboxUseCase {
  /** JOB-ID-OUTBOX-02. Returns the number of rows deleted. */
  int clean();
}
