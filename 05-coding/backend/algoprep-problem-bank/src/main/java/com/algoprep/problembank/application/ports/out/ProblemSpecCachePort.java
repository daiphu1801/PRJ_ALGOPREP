package com.algoprep.problembank.application.ports.out;

import java.util.UUID;

/** Invalidates only after successful commit; Redis failure must not undo persisted authoring. */
public interface ProblemSpecCachePort {
  void invalidateAfterCommit(UUID problemId);
}
