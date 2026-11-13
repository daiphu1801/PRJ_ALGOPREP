package com.algoprep.problembank.domain.ports.out;

import com.algoprep.problembank.domain.model.ProblemSpecReadView;
import java.util.Optional;
import java.util.UUID;

/** Access control is always read separately from live metadata, never from a spec cache. */
public interface HarnessProblemReadRepository {
  Optional<ProblemSpecReadView> findSpec(UUID problemId);

  boolean canRead(UUID problemId, UUID callerUserId, String baseCategory);
}
