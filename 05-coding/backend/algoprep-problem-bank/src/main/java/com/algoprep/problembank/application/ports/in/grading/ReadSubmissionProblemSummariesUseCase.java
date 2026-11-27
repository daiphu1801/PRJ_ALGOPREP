package com.algoprep.problembank.application.ports.in.grading;

import com.algoprep.problembank.domain.model.grading.SubmissionProblemSummary;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Historical summaries, including withdrawn problems (F2 API section 58). */
public interface ReadSubmissionProblemSummariesUseCase {
  List<SubmissionProblemSummary> summaries(Set<UUID> ids);
}
