package com.algoprep.aireview.application.query;

import com.algoprep.aireview.application.dto.BudgetStatus;
import com.algoprep.aireview.domain.model.BudgetConfig;
import com.algoprep.aireview.domain.model.BudgetPolicy;
import com.algoprep.aireview.domain.model.FeatureCode;
import com.algoprep.aireview.domain.ports.out.AiBudgetAdminRepository;
import com.algoprep.aireview.domain.ports.out.AiBudgetRepository;
import com.algoprep.common.domain.ports.out.ClockPort;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.EnumMap;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * BR-AI-05: used total, lock flag, forecast and the four-feature breakdown (endpoints 18 to 20).
 */
@Service
public class BudgetStatusAssembler {
  private static final int WINDOW_DAYS = 7;

  private final AiBudgetRepository budget;
  private final AiBudgetAdminRepository admin;
  private final ClockPort clock;

  BudgetStatusAssembler(AiBudgetRepository budget, AiBudgetAdminRepository admin, ClockPort clock) {
    this.budget = budget;
    this.admin = admin;
    this.clock = clock;
  }

  /** Reads the current state; call inside a transaction so the reads agree with each other. */
  public BudgetStatus status() {
    BudgetConfig config = budget.readConfig();
    int warnPercent = admin.readLimits().warnPercent();
    Instant periodAt = BudgetPolicy.periodStartAt(config.periodStart());
    Map<FeatureCode, Long> raw = admin.tokensByFeatureSince(periodAt);
    Map<FeatureCode, Long> breakdown = new EnumMap<>(FeatureCode.class);
    long used = 0;
    for (FeatureCode code : FeatureCode.values()) {
      long tokens = raw.getOrDefault(code, 0L);
      breakdown.put(code, tokens);
      used += tokens;
    }
    boolean locked = BudgetPolicy.locked(config, used);

    LocalDate today = BudgetPolicy.dayOf(clock.now());
    LocalDate windowStart = today.minusDays(WINDOW_DAYS - 1L);
    if (windowStart.isBefore(config.periodStart())) {
      windowStart = config.periodStart();
    }
    long days = ChronoUnit.DAYS.between(windowStart, today) + 1;
    long windowTokens =
        locked || config.limitTokens() <= 0
            ? 0
            : budget.usedTokensSince(BudgetPolicy.periodStartAt(windowStart));
    LocalDate runOut =
        windowTokens == 0
            ? null
            : BudgetPolicy.projectedRunOutDate(
                config.limitTokens() - used, windowTokens, days, today);
    return new BudgetStatus(
        config.limitTokens(),
        used,
        config.periodStart(),
        config.lockedAt(),
        locked,
        runOut,
        breakdown,
        warnPercent);
  }
}
