package com.algoprep.problembank.domain.ports.out;

import com.algoprep.problembank.domain.model.ProblemWriteState;
import com.algoprep.problembank.domain.model.StoredProblemSpec;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/** All writes join the caller's transaction; lock the parent before reading its specification. */
public interface ProblemSpecRepository {
  Optional<ProblemWriteState> lock(UUID problemId);

  Optional<StoredProblemSpec> find(UUID problemId);

  void save(UUID problemId, StoredProblemSpec spec);

  Instant touch(UUID problemId, UUID actorId, boolean wrapperSupported);

  /** Persists a complete event envelope in problem.outbox_events, never publishes before commit. */
  void appendEvent(UUID problemId, String envelope);
}
