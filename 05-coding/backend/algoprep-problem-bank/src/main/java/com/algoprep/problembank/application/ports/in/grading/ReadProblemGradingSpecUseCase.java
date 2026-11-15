package com.algoprep.problembank.application.ports.in.grading;

import com.algoprep.problembank.domain.model.grading.GradingSpecification;
import java.util.Optional;
import java.util.UUID;

public interface ReadProblemGradingSpecUseCase {
  Optional<GradingSpecification> loadGradingSpec(UUID problemId);
}
