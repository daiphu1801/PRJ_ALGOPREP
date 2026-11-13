package com.algoprep.identity.application.ports.in;

/** Anonymises self-deleted accounts after the 30 day grace period (JOB-ID-02). */
public interface AnonymizeDeletedAccountsUseCase {

  /**
   * @return accounts anonymised in this run
   */
  int anonymize();
}
