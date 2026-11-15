package com.algoprep.identity.application.ports.in;

/** JOB-ID-05: removes audit rows older than the retention, as the policy allows (BR-20). */
public interface PurgeExpiredAuditLogsUseCase {

  /**
   * @return rows deleted by this run
   */
  int purge();
}
