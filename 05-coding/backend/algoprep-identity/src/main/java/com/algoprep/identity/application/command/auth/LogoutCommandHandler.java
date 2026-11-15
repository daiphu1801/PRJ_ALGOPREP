package com.algoprep.identity.application.command.auth;

import com.algoprep.common.domain.ports.out.ClockPort;
import com.algoprep.identity.application.ports.in.auth.LogoutUseCase;
import com.algoprep.identity.application.ports.out.SessionRevocationPublisher;
import com.algoprep.identity.domain.model.auth.RefreshTokenSecrets;
import com.algoprep.identity.domain.model.auth.SessionRevoked;
import com.algoprep.identity.domain.ports.out.RefreshTokenRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Revokes the refresh token of the current session; a missing or unknown token is ignored. */
@Service
class LogoutCommandHandler implements LogoutUseCase {

  private final RefreshTokenRepository tokens;
  private final ClockPort clock;
  private final SessionRevocationPublisher revocations;

  LogoutCommandHandler(
      RefreshTokenRepository tokens, ClockPort clock, SessionRevocationPublisher revocations) {
    this.tokens = tokens;
    this.clock = clock;
    this.revocations = revocations;
  }

  @Override
  @Transactional
  public void logout(String rawRefreshToken) {
    if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
      return;
    }
    tokens
        .findByHashForUpdate(RefreshTokenSecrets.hash(rawRefreshToken))
        .ifPresent(
            token -> {
              tokens.revokeFamily(token.familyId(), clock.now());
              revocations.publish(new SessionRevoked(token.familyId()));
            });
  }
}
