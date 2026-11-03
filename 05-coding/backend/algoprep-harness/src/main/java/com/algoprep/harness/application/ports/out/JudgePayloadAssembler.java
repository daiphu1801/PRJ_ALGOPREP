package com.algoprep.harness.application.ports.out;

import com.algoprep.harness.application.dto.ExecutionPlan;
import com.algoprep.harness.application.dto.JudgePayload;

public interface JudgePayloadAssembler {
  JudgePayload assemble(ExecutionPlan plan);
}
