package com.algoprep.judge.application.ports.out;

import com.algoprep.judge.application.dto.SubmissionReads.Problem;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Historical metadata only. Failure hides current visibility and omits optional summaries. */
public interface ProblemSummaryReadPort {
  List<Problem> summaries(Set<UUID> problemIds);

  List<UUID> search(String keyword, int limit);

  Set<UUID> currentSamples(UUID problemId, Set<UUID> testcaseIds);
}
