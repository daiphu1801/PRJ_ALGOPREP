package com.algoprep.identity.application.ports.in.audit;

import com.algoprep.identity.domain.model.audit.LogRetentionSettings;

/** Reads the audit log retention settings (#51). */
public interface GetLogRetentionSettingsUseCase {
  LogRetentionSettings get();
}
