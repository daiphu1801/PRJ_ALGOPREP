package com.algoprep.identity.application.ports.in;

/** Removes refresh tokens that are dead and old enough to be useless for any statistic. */
public interface PurgeRefreshTokensUseCase {

  /**
   * @return number of rows deleted
   */
  int purge();
}
