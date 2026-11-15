package com.algoprep.identity.application.command;

import com.algoprep.common.domain.ports.out.ClockPort;
import com.algoprep.identity.application.ports.in.LogoutUseCase;
import com.algoprep.identity.domain.model.RefreshTokenSecrets;
import com.algoprep.identity.domain.ports.out.RefreshTokenRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Revokes the refresh token of the current session; a missing or unknown token is ignored. */
@Service
class LogoutCommandHandler implements LogoutUseCase {

  private final RefreshTokenRepository tokens;
  private final ClockPort clock;

  LogoutCommandHandler(RefreshTokenRepository tokens, ClockPort clock) {
    this.tokens = tokens;
    this.clock = clock;
  }

  @Override
  @Transactional
  public void logout(String rawRefreshToken) {
    if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
      return;
    }
    tokens
        .findByHashForUpdate(RefreshTokenSecrets.hash(rawRefreshToken))
        .filter(token -> !token.isRevoked())
        .ifPresent(token -> tokens.revoke(token.id(), clock.now()));
  }
}
