package com.algoprep.identity.application.ports.in;

/**
 * Removes self-registered accounts that never confirmed their e-mail, so an address cannot be held
 * by a registration nobody finished.
 */
public interface PurgePendingAccountsUseCase {

  /**
   * @return number of accounts deleted
   */
  int purge();
}
