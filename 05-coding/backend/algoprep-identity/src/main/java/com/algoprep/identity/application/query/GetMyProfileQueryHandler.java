package com.algoprep.identity.application.query;

import com.algoprep.common.exception.UnauthenticatedException;
import com.algoprep.identity.application.ports.in.GetMyProfileUseCase;
import com.algoprep.identity.domain.exception.IdentityErrorCodes;
import com.algoprep.identity.domain.model.MyProfile;
import com.algoprep.identity.domain.ports.out.MyProfileRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** The caller is the only subject: the id comes from the access token, never from the request. */
@Service
class GetMyProfileQueryHandler implements GetMyProfileUseCase {
  private final MyProfileRepository profiles;

  GetMyProfileQueryHandler(MyProfileRepository profiles) {
    this.profiles = profiles;
  }

  @Override
  @Transactional(readOnly = true)
  public MyProfile get(UUID userId) {
    return profiles
        .findById(userId)
        .orElseThrow(
            () ->
                new UnauthenticatedException(
                    IdentityErrorCodes.REFRESH_TOKEN_INVALID, "Account is not available"));
  }
}
