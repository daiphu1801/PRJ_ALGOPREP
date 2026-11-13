package com.algoprep.identity.application.command;

import com.algoprep.common.domain.ports.out.ClockPort;
import com.algoprep.common.exception.PermissionDeniedException;
import com.algoprep.identity.domain.exception.AccountDeactivatedException;
import com.algoprep.identity.domain.exception.IdentityErrorCodes;
import com.algoprep.identity.domain.model.UserAccount;
import com.algoprep.identity.domain.model.UserStatus;
import com.algoprep.identity.domain.ports.out.UserAccountRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;

/** Account lifecycle admission and activity tracking for the password login transaction. */
@Service
class LoginAccountService {
  private final UserAccountRepository users;
  private final ClockPort clock;
  private final AccountRestorationService restoration;

  LoginAccountService(
      UserAccountRepository users, ClockPort clock, AccountRestorationService restoration) {
    this.users = users;
    this.clock = clock;
    this.restoration = restoration;
  }

  Optional<UserAccount> findByEmail(String email) {
    return users.findByEmail(email);
  }

  void admitByStatus(UserAccount user) {
    if (user.status() == UserStatus.DEACTIVATED) {
      throw new AccountDeactivatedException(restoration.issue(user.id()).orElse(null));
    }
    if (user.status() == UserStatus.PENDING_VERIFICATION) {
      if (!user.mustChangePassword()) {
        throw new PermissionDeniedException(
            IdentityErrorCodes.EMAIL_NOT_VERIFIED, "Email has not been verified");
      }
      users.markActive(user.id());
    }
  }

  void touchLastActive(UUID userId) {
    users.touchLastActive(userId, clock.now());
  }
}
