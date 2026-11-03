package com.algoprep.judge.application.ports.in;

import com.algoprep.judge.application.dto.QueueSettingsChange;
import com.algoprep.judge.domain.model.AdminQueueSettings;
import java.util.UUID;

/** API #19: last-writer-wins settings, with one audit diff in the same transaction. */
public interface UpdateAdminQueueSettingsUseCase {
  AdminQueueSettings update(UUID actor, QueueSettingsChange requested);
}
