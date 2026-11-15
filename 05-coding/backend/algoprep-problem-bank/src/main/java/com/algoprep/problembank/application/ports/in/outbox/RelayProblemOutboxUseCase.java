package com.algoprep.problembank.application.ports.in.outbox;

public interface RelayProblemOutboxUseCase {
  /** JOB-PB-01. Returns the number of rows attempted. */
  int relay();
}
