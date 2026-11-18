package com.algoprep.judge.application.ports.in.grading;

import com.algoprep.judge.domain.model.grading.GeneratorSandboxLimits;

public interface GetGeneratorSandboxLimitsUseCase {
  GeneratorSandboxLimits getGeneratorSandboxLimits();
}
