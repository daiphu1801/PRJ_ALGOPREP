package com.algoprep.aireview.domain.ports.out;

import com.algoprep.aireview.domain.model.BudgetConfig;
import com.algoprep.aireview.domain.model.FeatureCode;
import com.algoprep.aireview.domain.model.LimitsConfig;
import com.algoprep.aireview.domain.model.LimitsPatch;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

/** Admin reads and writes of the budget singleton row, plus the period rollover (JOB-AI-03). */
public interface AiBudgetAdminRepository {
  LimitsConfig readLimits();

  /** Sum of tokens per feature since the instant; features without rows are absent. */
  Map<FeatureCode, Long> tokensByFeatureSince(Instant since);

  /** Singleton row locked with {@code FOR UPDATE} until the transaction ends. */
  BudgetConfig readConfigForUpdate();

  /** Stores the new limit and returns the previous one. */
  long updateLimit(long newLimit, UUID actor);

  void clearLock(UUID actor);

  /** Updates only the non-null components and returns the stored values. */
  LimitsConfig updateLimits(LimitsPatch patch, UUID actor);

  /** Transaction-scoped try-lock that keeps two instances from running the rollover together. */
  boolean tryRolloverLock();

  /** Moves {@code period_start} forward and clears the lock; returns the changed row count. */
  int rollover(LocalDate monthStart);
}
