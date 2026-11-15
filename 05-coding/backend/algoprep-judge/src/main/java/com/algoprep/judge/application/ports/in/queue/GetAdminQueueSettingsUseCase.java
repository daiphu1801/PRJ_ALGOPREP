package com.algoprep.judge.application.ports.in.queue;

import com.algoprep.judge.domain.model.queue.AdminQueueSettings;

/** API #18: current queue settings. */
public interface GetAdminQueueSettingsUseCase {
  AdminQueueSettings read();
}
