package com.algoprep.identity.application.ports.in;

import com.algoprep.identity.application.dto.UpdateMySettingsCommand;
import com.algoprep.identity.domain.model.MySettings;

/** Updates the sent settings atomically and returns the stored result (#15). */
public interface UpdateMySettingsUseCase {
  MySettings update(UpdateMySettingsCommand command);
}
