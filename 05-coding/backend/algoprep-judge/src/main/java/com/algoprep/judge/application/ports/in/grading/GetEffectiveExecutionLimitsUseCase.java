package com.algoprep.judge.application.ports.in.grading;

import com.algoprep.judge.application.dto.grading.ExecutionLimitsView;
import java.util.UUID;

public interface GetEffectiveExecutionLimitsUseCase {
  ExecutionLimitsView get(UUID problemId, String language);
}
