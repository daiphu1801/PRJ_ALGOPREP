package com.algoprep.problembank.application.ports.in;

import com.algoprep.problembank.domain.model.GradingSpecification;
import java.util.Optional;
import java.util.UUID;

public interface ReadProblemGradingSpecUseCase {
  Optional<GradingSpecification> loadGradingSpec(UUID problemId);
}
