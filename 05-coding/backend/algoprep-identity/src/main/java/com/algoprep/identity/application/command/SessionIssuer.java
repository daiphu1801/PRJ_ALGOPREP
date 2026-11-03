package com.algoprep.identity.application.command;

import com.algoprep.common.domain.ports.out.ClockPort;
import com.algoprep.common.domain.ports.out.IdGeneratorPort;
import com.algoprep.identity.application.dto.IssuedAccessToken;
import com.algoprep.identity.application.ports.out.AccessTokenPort;
import com.algoprep.identity.domain.model.RefreshTokenSecrets;
import com.algoprep.identity.domain.model.StoredRefreshToken;
import com.algoprep.identity.domain.model.UserAccount;
import com.algoprep.identity.domain.ports.out.RefreshTokenRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Opens a session: signs an access token and stores a new refresh token. Shared by login (new
 * family) and refresh (same family), so both produce tokens the same way.
 */
@Component
class SessionIssuer {

  /**
   * @param accessToken signed access token with its expiry
   * @param expiresInSeconds remaining access token lifetime
   * @param rawRefreshToken cookie value; only its hash is stored
   */
  record Session(IssuedAccessToken accessToken, long expiresInSeconds, String rawRefreshToken) {}

  private final AccessTokenPort accessTokens;
  private final RefreshTokenRepository refreshTokens;
  private final IdGeneratorPort ids;
  private final ClockPort clock;
  private final Duration refreshTtl;

  SessionIssuer(
      AccessTokenPort accessTokens,
      RefreshTokenRepository refreshTokens,
      IdGeneratorPort ids,
      ClockPort clock,
      @Value("${algoprep.security.refresh-ttl-days:30}") long refreshTtlDays) {
    this.accessTokens = accessTokens;
    this.refreshTokens = refreshTokens;
    this.ids = ids;
    this.clock = clock;
    this.refreshTtl = Duration.ofDays(refreshTtlDays);
  }

  Session openNewFamily(UserAccount user) {
    return open(user, ids.newId());
  }

  Session open(UserAccount user, UUID familyId) {
    Instant now = clock.now();
    IssuedAccessToken access =
        accessTokens.issue(user.id(), user.baseCategory(), user.roleCode(), familyId);
    String raw = RefreshTokenSecrets.newRawToken();
    StoredRefreshToken row =
        new StoredRefreshToken(ids.newId(), user.id(), familyId, now.plus(refreshTtl), null);
    refreshTokens.insert(row, RefreshTokenSecrets.hash(raw), now);
    long expiresIn = Duration.between(now, access.expiresAt()).getSeconds();
    return new Session(access, expiresIn, raw);
  }
}
