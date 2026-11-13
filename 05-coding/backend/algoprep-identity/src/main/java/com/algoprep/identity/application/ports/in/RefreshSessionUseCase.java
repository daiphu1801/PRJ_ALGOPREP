package com.algoprep.identity.application.ports.in;

import com.algoprep.identity.application.dto.RefreshResult;

/** Endpoint #3: rotate a refresh token and issue a new access token (DD BR-02). */
public interface RefreshSessionUseCase {

  /**
   * @param rawRefreshToken value read from the cookie, may be null when the cookie is absent
   * @return new access token and rotated refresh token value
   */
  RefreshResult refresh(String rawRefreshToken);
}
