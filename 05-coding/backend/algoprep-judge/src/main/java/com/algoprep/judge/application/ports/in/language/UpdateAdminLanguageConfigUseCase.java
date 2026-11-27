package com.algoprep.judge.application.ports.in.language;

import com.algoprep.judge.domain.model.language.AdminLanguageConfig;
import java.util.UUID;

/** Atomically saves all four groups with optimistic versions and audit, API #22. */
public interface UpdateAdminLanguageConfigUseCase {
  AdminLanguageConfig update(UUID actor, AdminLanguageConfig requested);
}
