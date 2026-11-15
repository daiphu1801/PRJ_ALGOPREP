package com.algoprep.identity.application.ports.in;

import com.algoprep.identity.application.dto.UpdateLogRetentionCommand;
import com.algoprep.identity.domain.model.LogRetentionSettings;

/** Edits the audit log retention settings and audits the change (#52). */
public interface UpdateLogRetentionSettingsUseCase {
  LogRetentionSettings update(UpdateLogRetentionCommand command);
}
