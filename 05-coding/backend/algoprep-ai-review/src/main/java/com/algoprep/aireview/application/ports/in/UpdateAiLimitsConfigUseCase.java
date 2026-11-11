package com.algoprep.aireview.application.ports.in;

import com.algoprep.aireview.domain.model.LimitsConfig;
import com.algoprep.aireview.domain.model.LimitsPatch;
import java.util.UUID;

public interface UpdateAiLimitsConfigUseCase {
  LimitsConfig update(UUID actor, LimitsPatch patch);
}
