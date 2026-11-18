package com.algoprep.aireview.application.ports.in.budget;

import com.algoprep.aireview.domain.model.budget.LimitsConfig;

public interface GetAiLimitsConfigUseCase {
  LimitsConfig read();
}
