package com.algoprep.identity.application.query;

import com.algoprep.common.exception.UnauthenticatedException;
import com.algoprep.identity.application.ports.in.GetMySettingsUseCase;
import com.algoprep.identity.domain.exception.IdentityErrorCodes;
import com.algoprep.identity.domain.model.MySettings;
import com.algoprep.identity.domain.ports.out.MySettingsRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** The caller is the only subject: the id comes from the access token. */
@Service
class GetMySettingsQueryHandler implements GetMySettingsUseCase {
  private final MySettingsRepository settings;

  GetMySettingsQueryHandler(MySettingsRepository settings) {
    this.settings = settings;
  }

  @Override
  @Transactional(readOnly = true)
  public MySettings get(UUID userId) {
    return settings
        .find(userId)
        .orElseThrow(
            () ->
                new UnauthenticatedException(
                    IdentityErrorCodes.REFRESH_TOKEN_INVALID, "Account is not available"));
  }
}
