package com.algoprep.identity.application.ports.out;

import com.algoprep.identity.application.dto.IssuedAccessToken;
import com.algoprep.identity.domain.model.BaseCategory;
import java.util.UUID;

/**
 * Issues short-lived access tokens. Claims carry identity only; the permission matrix is looked up
 * on every request so a changed cell takes effect immediately (BD security section 1).
 */
public interface AccessTokenPort {

  /**
   * @param userId subject of the token
   * @param baseCategory layer-1 role category
   * @param roleCode code of the user's role
   * @param sessionId refresh token family of the session, so "change password" can keep it (BR-06)
   * @return the signed token and its expiry
   */
  IssuedAccessToken issue(UUID userId, BaseCategory baseCategory, String roleCode, UUID sessionId);
}
