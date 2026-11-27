package com.algoprep.aireview.application.ports.in.budget;

import com.algoprep.aireview.application.dto.budget.BudgetStatus;
import java.util.UUID;

public interface UnlockAiTokenBudgetUseCase {
  BudgetStatus unlock(UUID actor);
}
