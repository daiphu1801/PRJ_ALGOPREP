package com.algoprep.judge.application.ports.in;

import com.algoprep.judge.domain.model.AdminQueueSettings;

/** API #18: current queue settings. */
public interface GetAdminQueueSettingsUseCase {
  AdminQueueSettings read();
}
