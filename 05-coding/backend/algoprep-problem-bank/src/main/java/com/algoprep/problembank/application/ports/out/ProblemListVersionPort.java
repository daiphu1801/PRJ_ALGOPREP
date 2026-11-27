package com.algoprep.problembank.application.ports.out;

/** Registers a best-effort invalidation after the database transaction commits. */
public interface ProblemListVersionPort {
  void incrementAfterCommit();
}
