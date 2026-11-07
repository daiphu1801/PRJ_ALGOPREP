package com.algoprep.problembank.domain.ports.out;

import com.algoprep.problembank.domain.model.ProblemContentData;
import com.algoprep.problembank.domain.model.ProblemContentSnapshot;
import java.time.Instant;
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

  /** The three AI authoring columns (DD F2 section 39); returns the new {@code updated_at}. */
  Instant writeAiContext(
      UUID problemId, UUID actorId, String brief, boolean noFullCode, boolean questionOnly);

  /** Sets {@code duplicated_from_problem_id} on a freshly created copy (DD F2 section 36.5). */
  void markDuplicatedFrom(UUID problemId, UUID sourceId);
}
