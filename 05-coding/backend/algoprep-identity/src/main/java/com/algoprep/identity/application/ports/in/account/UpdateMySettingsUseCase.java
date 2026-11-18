package com.algoprep.identity.application.ports.in.account;

import com.algoprep.identity.application.dto.account.UpdateMySettingsCommand;
import com.algoprep.identity.domain.model.account.MySettings;

/** Updates the sent settings atomically and returns the stored result (#15). */
public interface UpdateMySettingsUseCase {
  MySettings update(UpdateMySettingsCommand command);
}
