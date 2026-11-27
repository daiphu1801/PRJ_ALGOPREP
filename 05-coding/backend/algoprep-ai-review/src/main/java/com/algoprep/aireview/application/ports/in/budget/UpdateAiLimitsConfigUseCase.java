package com.algoprep.aireview.application.ports.in.budget;

import com.algoprep.aireview.domain.model.budget.LimitsConfig;
import com.algoprep.aireview.domain.model.budget.LimitsPatch;
import java.util.UUID;

public interface UpdateAiLimitsConfigUseCase {
  LimitsConfig update(UUID actor, LimitsPatch patch);
}
