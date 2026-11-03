package com.algoprep.identity.application.ports.in;

import com.algoprep.identity.domain.model.MySettings;
import java.util.UUID;

/** Reads the caller's account settings (#14). */
public interface GetMySettingsUseCase {
  MySettings get(UUID userId);
}
