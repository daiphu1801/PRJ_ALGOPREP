package com.algoprep.problembank.domain.ports.out;

import com.algoprep.problembank.domain.model.ProblemContentData;
import com.algoprep.problembank.domain.model.ProblemContentSnapshot;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Content writes join the caller transaction. Parent row lock is shared with SaveProblemSpec. */
public interface ProblemContentRepository {
  Optional<ProblemContentSnapshot> lock(UUID problemId);

  /** Serializes reference validation with S1 catalogue membership changes; level then topic. */
  void lockCatalogues();

  Optional<UUID> firstLevel();

  boolean cataloguesExist(UUID levelId, List<UUID> topicIds);

  /** Ordinary draft only; duplication and MinIO copying belong to a separate orchestration. */
  UUID create(UUID actorId, ProblemContentData content);

  ProblemContentSnapshot write(UUID problemId, UUID actorId, ProblemContentData content);
}
