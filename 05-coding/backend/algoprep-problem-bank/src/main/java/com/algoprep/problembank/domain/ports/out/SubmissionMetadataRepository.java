package com.algoprep.problembank.domain.ports.out;

import com.algoprep.problembank.domain.model.SubmissionProblemSummary;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Metadata only: never reads input, expected output, object keys or source. */
public interface SubmissionMetadataRepository {
  List<SubmissionProblemSummary> summaries(Set<UUID> ids);

  List<UUID> search(String keyword, int limit);

  Set<UUID> samples(UUID problemId, Set<UUID> testcaseIds);
}
