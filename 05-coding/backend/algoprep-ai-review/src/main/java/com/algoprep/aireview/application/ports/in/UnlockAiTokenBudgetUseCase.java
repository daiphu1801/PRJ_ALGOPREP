package com.algoprep.aireview.application.ports.in;

import com.algoprep.aireview.application.dto.BudgetStatus;
import java.util.UUID;

public interface UnlockAiTokenBudgetUseCase {
  BudgetStatus unlock(UUID actor);
}
