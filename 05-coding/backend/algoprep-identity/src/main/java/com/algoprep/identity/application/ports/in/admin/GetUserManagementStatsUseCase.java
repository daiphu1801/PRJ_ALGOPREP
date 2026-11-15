package com.algoprep.identity.application.ports.in.admin;

/** Number of accounts waiting for e-mail verification (#62). */
public interface GetUserManagementStatsUseCase {
  long pendingVerificationCount();
}
