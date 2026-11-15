package com.algoprep.judge.application.ports.in;

import com.algoprep.judge.application.dto.ExecutionLimitsView;
import java.util.UUID;

public interface GetEffectiveExecutionLimitsUseCase {
  ExecutionLimitsView get(UUID problemId, String language);
}
