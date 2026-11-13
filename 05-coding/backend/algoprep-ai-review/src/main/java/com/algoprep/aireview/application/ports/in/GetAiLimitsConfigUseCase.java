package com.algoprep.aireview.application.ports.in;

import com.algoprep.aireview.domain.model.LimitsConfig;

public interface GetAiLimitsConfigUseCase {
  LimitsConfig read();
}
