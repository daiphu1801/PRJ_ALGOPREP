package com.algoprep.identity.application.command;

import com.algoprep.common.domain.ports.out.ClockPort;
import com.algoprep.common.exception.UnauthenticatedException;
import com.algoprep.identity.application.dto.RefreshResult;
import com.algoprep.identity.application.ports.in.RefreshSessionUseCase;
import com.algoprep.identity.domain.exception.IdentityErrorCodes;
import com.algoprep.identity.domain.model.AuditEntry;
import com.algoprep.identity.domain.model.RefreshTokenSecrets;
import com.algoprep.identity.domain.model.StoredRefreshToken;
import com.algoprep.identity.domain.model.UserAccount;
import com.algoprep.identity.domain.model.UserStatus;
import com.algoprep.identity.domain.ports.out.AuditLogPort;
import com.algoprep.identity.domain.ports.out.RefreshTokenRepository;
import com.algoprep.identity.domain.ports.out.UserAccountRepository;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Refresh token rotation with reuse detection (DD BR-02).
 *
 * <p>The transaction must <strong>commit</strong> when reuse is detected: the family revocation and
 * the audit row are the whole point, and the exception that tells the client 401 would otherwise
 * roll them back. Every other {@link UnauthenticatedException} is thrown before any write, so not
 * rolling back for them changes nothing.
 */
@Service
class RefreshSessionCommandHandler implements RefreshSessionUseCase {

  private final RefreshTokenRepository tokens;
  private final UserAccountRepository users;
  private final AuditLogPort audit;
  private final SessionIssuer sessions;
  private final ClockPort clock;

  RefreshSessionCommandHandler(
      RefreshTokenRepository tokens,
      UserAccountRepository users,
      AuditLogPort audit,
      SessionIssuer sessions,
      ClockPort clock) {
    this.tokens = tokens;
    this.users = users;
    this.audit = audit;
    this.sessions = sessions;
    this.clock = clock;
  }

  @Override
  @Transactional(noRollbackFor = UnauthenticatedException.class)
  public RefreshResult refresh(String rawRefreshToken) {
    if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
      throw invalid();
    }
    Instant now = clock.now();
    StoredRefreshToken token =
        tokens
            .findByHashForUpdate(RefreshTokenSecrets.hash(rawRefreshToken))
            .orElseThrow(RefreshSessionCommandHandler::invalid);

    if (token.isRevoked()) {
      tokens.revokeFamily(token.familyId(), now);
      audit.record(
          new AuditEntry(
              token.userId(),
              "SECURITY_TOKEN_REUSE_DETECTED",
              "REFRESH_TOKEN",
              token.id().toString(),
              null,
              "{\"familyId\":\"" + token.familyId() + "\"}"));
      throw new UnauthenticatedException(
          IdentityErrorCodes.REFRESH_TOKEN_REUSED, "Refresh token was already used");
    }
    if (token.isExpiredAt(now)) {
      throw invalid();
    }
    UserAccount user =
        users
            .findById(token.userId())
            .filter(u -> u.status() == UserStatus.ACTIVE)
            .orElseThrow(RefreshSessionCommandHandler::invalid);

    tokens.revoke(token.id(), now);
    SessionIssuer.Session session = sessions.open(user, token.familyId());
    return new RefreshResult(
        session.accessToken().value(), session.expiresInSeconds(), session.rawRefreshToken());
  }

  private static UnauthenticatedException invalid() {
    return new UnauthenticatedException(
        IdentityErrorCodes.REFRESH_TOKEN_INVALID, "Refresh token is invalid or expired");
  }
}
