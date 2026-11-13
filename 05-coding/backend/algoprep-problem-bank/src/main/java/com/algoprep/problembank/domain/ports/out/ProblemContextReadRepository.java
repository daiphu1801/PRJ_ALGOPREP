package com.algoprep.problembank.domain.ports.out;

import com.algoprep.problembank.domain.model.ProblemContextView;
import java.util.Optional;
import java.util.UUID;

public interface ProblemContextReadRepository {
  /**
   * Empty unless the problem is {@code PUBLISHED AND NOT deleted}. {@code language} picks the
   * per-language function name override and may be null.
   */
  Optional<ProblemContextView> findVisible(UUID problemId, String language);
}
