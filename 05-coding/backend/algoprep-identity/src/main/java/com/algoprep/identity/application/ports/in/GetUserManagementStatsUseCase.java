package com.algoprep.identity.application.ports.in;

/** Number of accounts waiting for e-mail verification (#62). */
public interface GetUserManagementStatsUseCase {
  long pendingVerificationCount();
}
