package com.algoprep.problembank.application.ports.in.grading;

import com.algoprep.problembank.domain.model.grading.GradingSpecification;
import java.util.Optional;
import java.util.UUID;

public interface ReadProblemGradingSpecUseCase {
  /** Internal reads preserve withdrawn metadata for already admitted worker jobs. */
  Optional<GradingSpecification> loadGradingSpec(UUID problemId);

  /** Learner reads exclude unpublished/deleted rows before validating grading data. */
  Optional<GradingSpecification> loadPublishedGradingSpec(UUID problemId);
}
