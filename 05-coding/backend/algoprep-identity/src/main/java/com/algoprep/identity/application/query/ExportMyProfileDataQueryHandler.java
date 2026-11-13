package com.algoprep.identity.application.query;

import com.algoprep.common.exception.RateLimitExceededException;
import com.algoprep.common.exception.UnauthenticatedException;
import com.algoprep.identity.application.dto.PersonalDataExport;
import com.algoprep.identity.application.ports.in.ExportMyProfileDataUseCase;
import com.algoprep.identity.application.ports.out.ExportRateLimitPort;
import com.algoprep.identity.domain.exception.IdentityErrorCodes;
import com.algoprep.identity.domain.ports.out.MyProfileRepository;
import com.algoprep.identity.domain.ports.out.MySettingsRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** The caller is the only subject: the id comes from the access token, never from the request. */
@Service
class ExportMyProfileDataQueryHandler implements ExportMyProfileDataUseCase {
  static final String SCOPE = "profile";
  private final ExportRateLimitPort limit;
  private final MyProfileRepository profiles;
  private final MySettingsRepository settings;

  ExportMyProfileDataQueryHandler(
      ExportRateLimitPort limit, MyProfileRepository profiles, MySettingsRepository settings) {
    this.limit = limit;
    this.profiles = profiles;
    this.settings = settings;
  }

  @Override
  @Transactional(readOnly = true)
  public PersonalDataExport export(UUID userId) {
    if (!limit.tryAcquire(userId, SCOPE)) {
      throw new RateLimitExceededException(
          "IDT-V06", "This export was already requested in the last 5 minutes");
    }
    return new PersonalDataExport(
        profiles.findById(userId).orElseThrow(ExportMyProfileDataQueryHandler::gone),
        settings.find(userId).orElseThrow(ExportMyProfileDataQueryHandler::gone));
  }

  private static UnauthenticatedException gone() {
    return new UnauthenticatedException(
        IdentityErrorCodes.REFRESH_TOKEN_INVALID, "Account is not available");
  }
}
