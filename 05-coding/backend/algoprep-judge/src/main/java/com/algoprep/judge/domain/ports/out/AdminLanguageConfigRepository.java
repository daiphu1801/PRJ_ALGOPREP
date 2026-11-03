package com.algoprep.judge.domain.ports.out;

import com.algoprep.judge.domain.model.AdminLanguageConfig;
import java.util.UUID;

/** Own-schema configuration only; missing rows fail closed, never fabricated defaults. */
public interface AdminLanguageConfigRepository {
  AdminLanguageConfig read();

  /** Locks all three language rows in order, then the three singleton rows. */
  AdminLanguageConfig lock();

  /** Updates only changed rows; the caller holds all locks until audit commits. */
  void saveChanges(AdminLanguageConfig before, AdminLanguageConfig after, UUID actor);
}
