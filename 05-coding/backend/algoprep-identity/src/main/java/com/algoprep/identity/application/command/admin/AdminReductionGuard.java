package com.algoprep.identity.application.command;

import com.algoprep.common.exception.DomainRuleViolationException;
import com.algoprep.common.exception.InvalidRequestException;
import com.algoprep.common.exception.ResourceNotFoundException;
import com.algoprep.identity.domain.exception.IdentityErrorCodes;
import com.algoprep.identity.domain.model.AdminUserItem;
import com.algoprep.identity.domain.ports.out.AccountLifecycleRepository;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * BR-15 for an action that takes an active ADMIN out of the admin set (#36, #37) and the shared "no
 * such account" answer of #36 to #38.
 */
@Component
class AdminReductionGuard {
  private final AccountLifecycleRepository accounts;

  AdminReductionGuard(AccountLifecycleRepository accounts) {
    this.accounts = accounts;
  }

  static ResourceNotFoundException notFound() {
    return new ResourceNotFoundException(
        IdentityErrorCodes.USER_NOT_FOUND, "Account is not available");
  }

  static boolean isActiveAdmin(AdminUserItem user) {
    return "ADMIN".equals(user.baseCategory()) && "ACTIVE".equals(user.status());
  }

  /**
   * Refuses the last active admin first ({@code IDT-432}; the admin rows are locked in id order so
   * two admins cannot both pass), then demands the explicit acknowledgement when the admin hits
   * their own account ({@code IDT-V21}).
   */
  void guard(UUID actorId, UUID targetId, Object acknowledgeSelfImpact) {
    if (accounts.isLastActiveAdmin(targetId)) {
      throw new DomainRuleViolationException(
          IdentityErrorCodes.LAST_ADMIN_PROTECTED, "At least one active administrator must remain");
    }
    if (actorId.equals(targetId) && !Boolean.TRUE.equals(acknowledgeSelfImpact)) {
      throw new InvalidRequestException(
          IdentityErrorCodes.SELF_IMPACT_NOT_ACKNOWLEDGED, "Acknowledge the effect on yourself");
    }
  }
}
