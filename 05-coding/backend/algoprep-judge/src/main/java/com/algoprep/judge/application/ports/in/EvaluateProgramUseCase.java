package com.algoprep.judge.application.ports.in;

import com.algoprep.judge.application.dto.ExecutionModels.EvaluationRequest;
import com.algoprep.judge.application.dto.ExecutionModels.EvaluationResult;

/** Component contract independent of the deferred persisted submission workflow. */
public interface EvaluateProgramUseCase {
  EvaluationResult evaluate(EvaluationRequest request);
}
