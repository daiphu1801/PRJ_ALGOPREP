package com.algoprep.identity.application.ports.in;

/** Endpoint #4: end the current session by revoking its refresh token. */
public interface LogoutUseCase {

  /**
   * Idempotent: an absent, unknown or already revoked token is not an error.
   *
   * @param rawRefreshToken value read from the cookie, may be null
   */
  void logout(String rawRefreshToken);
}
