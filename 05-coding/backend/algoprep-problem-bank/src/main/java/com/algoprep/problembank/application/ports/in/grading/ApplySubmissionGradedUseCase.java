package com.algoprep.problembank.application.ports.in;

import com.algoprep.problembank.domain.model.ProblemStatsModels.GradedSubmission;

/** BR-20: counts a {@code SubmissionGraded} event into {@code problem_stats}, once per event id. */
public interface ApplySubmissionGradedUseCase {
  /** True when the event was new, false when it was a redelivery. */
  boolean apply(GradedSubmission submission);
}
