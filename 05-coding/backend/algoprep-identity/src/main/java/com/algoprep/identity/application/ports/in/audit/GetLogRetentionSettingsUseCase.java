package com.algoprep.identity.application.ports.in;

import com.algoprep.identity.domain.model.LogRetentionSettings;

/** Reads the audit log retention settings (#51). */
public interface GetLogRetentionSettingsUseCase {
  LogRetentionSettings get();
}
