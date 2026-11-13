package com.algoprep.identity.application.query;

import com.algoprep.identity.application.ports.in.GetUserManagementStatsUseCase;
import com.algoprep.identity.domain.ports.out.AdminUserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Pending-verification count of #62; independent of the list filters. */
@Service
class GetUserManagementStatsQueryHandler implements GetUserManagementStatsUseCase {
  private final AdminUserRepository users;

  GetUserManagementStatsQueryHandler(AdminUserRepository users) {
    this.users = users;
  }

  @Override
  @Transactional(readOnly = true)
  public long pendingVerificationCount() {
    return users.countPendingVerification();
  }
}
