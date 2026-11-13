package com.algoprep.aireview.application.command;

import com.algoprep.aireview.application.ports.in.CheckAiBudgetUseCase;
import com.algoprep.aireview.domain.exception.AiBudgetExceededException;
import com.algoprep.aireview.domain.model.BudgetConfig;
import com.algoprep.aireview.domain.model.BudgetPolicy;
import com.algoprep.aireview.domain.model.CallerCategory;
import com.algoprep.aireview.domain.ports.out.AiBudgetRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * BR-AI-05 step 3. The gate heals itself: if the total already reached the limit but {@code
 * locked_at} is still null (an admin lowered the limit), it sets the lock before rejecting. The
 * exception must not roll that update back, hence {@code noRollbackFor}.
 */
@Service
class CheckAiBudgetCommandHandler implements CheckAiBudgetUseCase {
  private final AiBudgetRepository budget;

  CheckAiBudgetCommandHandler(AiBudgetRepository budget) {
    this.budget = budget;
  }

  @Override
  @Transactional(noRollbackFor = AiBudgetExceededException.class)
  public void check(UUID userId, CallerCategory baseCategory) {
    if (baseCategory == CallerCategory.ADMIN) {
      return;
    }
    BudgetConfig config = budget.readConfig();
    if (config.lockedAt() != null) {
      throw new AiBudgetExceededException();
    }
    long used = budget.usedTokensSince(BudgetPolicy.periodStartAt(config.periodStart()));
    if (BudgetPolicy.exhausted(config.limitTokens(), used)) {
      budget.lockIfUnlocked();
      throw new AiBudgetExceededException();
    }
  }
}
