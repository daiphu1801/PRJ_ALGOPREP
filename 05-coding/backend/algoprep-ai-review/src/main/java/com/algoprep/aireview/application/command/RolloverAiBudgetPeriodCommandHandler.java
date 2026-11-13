package com.algoprep.aireview.application.command;

import com.algoprep.aireview.application.dto.RolloverOutcome;
import com.algoprep.aireview.application.ports.in.RolloverAiBudgetPeriodUseCase;
import com.algoprep.aireview.domain.model.BudgetConfig;
import com.algoprep.aireview.domain.model.BudgetPolicy;
import com.algoprep.aireview.domain.ports.out.AiBudgetAdminRepository;
import com.algoprep.aireview.domain.ports.out.AiBudgetRepository;
import com.algoprep.common.domain.ports.out.ClockPort;
import java.time.LocalDate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * JOB-AI-03: moves {@code period_start} to the first day of the current UTC+7 month and clears the
 * lock. Idempotent through the {@code period_start <} condition; the advisory try-lock stops two
 * instances from running together. Writes no audit row (no acting user, DD jobs F5 section 3).
 */
@Service
@Transactional
class RolloverAiBudgetPeriodCommandHandler implements RolloverAiBudgetPeriodUseCase {
  private final AiBudgetAdminRepository admin;
  private final AiBudgetRepository budget;
  private final ClockPort clock;

  RolloverAiBudgetPeriodCommandHandler(
      AiBudgetAdminRepository admin, AiBudgetRepository budget, ClockPort clock) {
    this.admin = admin;
    this.budget = budget;
    this.clock = clock;
  }

  @Override
  public RolloverOutcome rollover() {
    LocalDate month = BudgetPolicy.monthStartOf(clock.now());
    if (!admin.tryRolloverLock()) {
      return new RolloverOutcome(true, false, month, false);
    }
    BudgetConfig before = budget.readConfig();
    boolean changed = admin.rollover(month) > 0;
    return new RolloverOutcome(
        false,
        changed,
        changed ? month : before.periodStart(),
        changed && before.lockedAt() != null);
  }
}
