package com.algoprep.problembank.domain.ports.out;

import com.algoprep.problembank.domain.model.ProblemStatsModels.ProblemStatsRow;
import java.util.Optional;
import java.util.UUID;

/** The {@code problem_stats} counters and the {@code consumed_events} de-duplication keys. */
public interface ProblemStatsRepository {
  /** Records the event id; false when it was recorded before, so the event must be skipped. */
  boolean markConsumed(UUID eventId);

  /**
   * One submission for the problem: creates or updates the row, counters and rate in one statement.
   * False when the problem does not exist (nothing is written).
   */
  boolean addSubmission(UUID problemId, boolean accepted);

  /** The problem with its counters, absent when the problem does not exist or is deleted. */
  Optional<ProblemStatsRow> find(UUID problemId);
}
