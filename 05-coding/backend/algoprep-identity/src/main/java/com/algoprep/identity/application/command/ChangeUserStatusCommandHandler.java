package com.algoprep.identity.application.command;

import com.algoprep.common.exception.DomainRuleViolationException;
import com.algoprep.identity.application.dto.ChangeUserStatusCommand;
import com.algoprep.identity.application.ports.in.ChangeUserStatusUseCase;
import com.algoprep.identity.application.ports.out.EmailPort;
import com.algoprep.identity.domain.exception.IdentityErrorCodes;
import com.algoprep.identity.domain.model.AdminUserItem;
import com.algoprep.identity.domain.model.AuditEntry;
import com.algoprep.identity.domain.ports.out.AdminUserRepository;
import com.algoprep.identity.domain.ports.out.AuditLogPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Locks or unlocks an account (#37). A lock sets {@code DEACTIVATED} with source {@code ADMIN},
 * revokes every refresh token (the access token stops at once, the converter requires {@code
 * ACTIVE}) and may e-mail the reason, which is then mandatory. An unlock writes only the audit row,
 * and is refused for an anonymised account ({@code IDT-448}). Asking for the state the account
 * already has changes nothing.
 */
@Service
class ChangeUserStatusCommandHandler implements ChangeUserStatusUseCase {
  static final int REASON_MAX = 500;

  private final AdminUserRepository users;
  private final AdminReductionGuard guard;
  private final AuditLogPort audit;
  private final EmailPort email;

  ChangeUserStatusCommandHandler(
      AdminUserRepository users, AdminReductionGuard guard, AuditLogPort audit, EmailPort email) {
    this.users = users;
    this.guard = guard;
    this.audit = audit;
    this.email = email;
  }

  @Override
  @Transactional
  public AdminUserItem change(ChangeUserStatusCommand command) {
    boolean lock = isLock(command.targetStatus());
    String reason = reason(command.reason());
    boolean notify = Boolean.TRUE.equals(flag(command.notifyByEmail()));
    if (lock && notify && reason == null) {
      throw invalid("A reason is required when the user is notified");
    }
    AdminUserItem peek = users.find(command.userId()).orElseThrow(AdminReductionGuard::notFound);
    if (!lock && peek.anonymized()) {
      throw new DomainRuleViolationException(
          IdentityErrorCodes.ACCOUNT_ANONYMIZED, "An anonymised account cannot be unlocked");
    }
    if (lock && AdminReductionGuard.isActiveAdmin(peek)) {
      guard.guard(command.actorId(), command.userId(), command.acknowledgeSelfImpact());
    }
    AdminUserItem current = users.lock(command.userId()).orElseThrow(AdminReductionGuard::notFound);
    boolean deactivated = "DEACTIVATED".equals(current.status());
    if (lock == deactivated) {
      return current;
    }
    if (lock) {
      users.deactivateByAdmin(command.userId());
      users.revokeAllSessions(command.userId());
      audit.record(
          entry(
              command,
              "USER_LOCKED",
              RbacAuditJson.of(
                  "status", "DEACTIVATED", "reason", reason, "notifiedByEmail", notify)));
      if (notify) {
        email.send(
            current.email(),
            "AlgoPrep - Tài khoản của bạn đã bị khoá",
            "Tài khoản "
                + current.email()
                + " đã bị quản trị viên khoá. Lý do: "
                + reason
                + ". Hãy liên hệ quản trị viên nếu cần mở khoá.");
      }
    } else {
      users.reactivate(command.userId());
      audit.record(entry(command, "USER_UNLOCKED", RbacAuditJson.of("status", "ACTIVE")));
    }
    return users.find(command.userId()).orElseThrow(AdminReductionGuard::notFound);
  }

  private static AuditEntry entry(ChangeUserStatusCommand command, String action, String after) {
    return new AuditEntry(
        command.actorId(), action, "USER", command.userId().toString(), null, after);
  }

  private static boolean isLock(Object target) {
    if ("DEACTIVATED".equals(target)) {
      return true;
    }
    if ("ACTIVE".equals(target)) {
      return false;
    }
    throw invalid("targetStatus must be ACTIVE or DEACTIVATED");
  }

  /** Trimmed reason, null when absent or blank; a non-string or over-long reason is invalid. */
  private static String reason(Object raw) {
    if (raw == null) {
      return null;
    }
    if (!(raw instanceof String text) || text.trim().length() > REASON_MAX) {
      throw invalid("reason must be text of at most 500 characters");
    }
    return text.isBlank() ? null : text.trim();
  }

  private static Boolean flag(Object raw) {
    if (raw == null || raw instanceof Boolean) {
      return (Boolean) raw;
    }
    throw invalid("notifyByEmail must be a boolean");
  }

  private static DomainRuleViolationException invalid(String message) {
    return new DomainRuleViolationException(IdentityErrorCodes.ADMIN_USER_REQUEST_INVALID, message);
  }
}
