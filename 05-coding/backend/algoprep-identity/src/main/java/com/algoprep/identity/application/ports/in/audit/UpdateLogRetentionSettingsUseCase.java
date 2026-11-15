package com.algoprep.identity.application.ports.in.audit;

import com.algoprep.identity.application.dto.audit.UpdateLogRetentionCommand;
import com.algoprep.identity.domain.model.audit.LogRetentionSettings;

/** Edits the audit log retention settings and audits the change (#52). */
public interface UpdateLogRetentionSettingsUseCase {
  LogRetentionSettings update(UpdateLogRetentionCommand command);
}
