package com.algoprep.aireview.domain.ports.out;

import com.algoprep.aireview.domain.model.BudgetConfig;
import java.time.Instant;

/** Budget singleton row and the usage total of the current period. */
public interface AiBudgetRepository {
  BudgetConfig readConfig();

  /** Sum of prompt + completion tokens of every ledger row created at or after {@code since}. */
  long usedTokensSince(Instant since);

  /** Sets {@code locked_at = now()} only if it is still null; true when this call set it. */
  boolean lockIfUnlocked();
}
