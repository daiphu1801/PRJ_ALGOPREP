package com.algoprep.judge.application.ports.in.queue;

import com.algoprep.judge.application.dto.queue.QueueSettingsChange;
import com.algoprep.judge.domain.model.queue.AdminQueueSettings;
import java.util.UUID;

/** API #19: last-writer-wins settings, with one audit diff in the same transaction. */
public interface UpdateAdminQueueSettingsUseCase {
  AdminQueueSettings update(UUID actor, QueueSettingsChange requested);
}
