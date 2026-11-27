package com.algoprep.aireview.application.ports.in.budget;

import com.algoprep.aireview.application.dto.budget.BudgetStatus;

public interface GetAiTokenBudgetUseCase {
  BudgetStatus status();
}
