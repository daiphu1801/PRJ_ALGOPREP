package com.algoprep.identity.application.command;

import com.algoprep.common.exception.DomainRuleViolationException;
import com.algoprep.common.exception.RateLimitExceededException;
import com.algoprep.common.exception.UnauthenticatedException;
import com.algoprep.identity.application.dto.ChangeMyPasswordCommand;
import com.algoprep.identity.application.ports.in.ChangeMyPasswordUseCase;
import com.algoprep.identity.application.ports.out.LoginAttemptPort;
import com.algoprep.identity.application.ports.out.PasswordHasherPort;
import com.algoprep.identity.domain.exception.IdentityErrorCodes;
import com.algoprep.identity.domain.model.AuditEntry;
import com.algoprep.identity.domain.model.PasswordChangeAccount;
import com.algoprep.identity.domain.ports.out.AuditLogPort;
import com.algoprep.identity.domain.ports.out.PasswordChangeRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Change password for an account that already has one (BR-06): new password rules, then the current
 * password, then hash + revoke every session except the caller's.
 *
 * <p>ponytail: the first-password branch for OAuth-only accounts (BR-05) is not built; no such
 * account exists before task #15, so a null hash is rejected like a wrong current password.
 */
@Service
class ChangeMyPasswordCommandHandler implements ChangeMyPasswordUseCase {
  private final PasswordChangeRepository accounts;
  private final PasswordHasherPort passwords;
  private final RegistrationValidation validation;
  private final AuditLogPort audit;
  private final LoginAttemptPort attempts;

  ChangeMyPasswordCommandHandler(
      PasswordChangeRepository accounts,
      PasswordHasherPort passwords,
      RegistrationValidation validation,
      AuditLogPort audit,
      LoginAttemptPort attempts) {
    this.accounts = accounts;
    this.passwords = passwords;
    this.validation = validation;
    this.audit = audit;
    this.attempts = attempts;
  }

  @Override
  @Transactional
  public void change(ChangeMyPasswordCommand command) {
    PasswordChangeAccount account =
        accounts
            .lockById(command.userId())
            .orElseThrow(
                () ->
                    new UnauthenticatedException(
                        IdentityErrorCodes.REFRESH_TOKEN_INVALID, "Account is not available"));
    String password =
        validation.password(command.newPassword(), account.email(), account.user().displayName());
    // A stolen access token must not be able to guess the current password: the same five-in-15-
    // minutes budget as the login (BR-01), keyed by account instead of by e-mail and address.
    String budgetKey = "pwchange:" + command.userId();
    if (attempts.isBlocked(budgetKey, "-")) {
      throw new RateLimitExceededException(
          IdentityErrorCodes.LOGIN_RATE_LIMITED, "Too many failed attempts");
    }
    if (!passwords.matches(command.currentPassword(), account.user().passwordHash())) {
      attempts.recordFailure(budgetKey, "-");
      throw new DomainRuleViolationException(
          IdentityErrorCodes.CURRENT_PASSWORD_INCORRECT, "Current password is incorrect");
    }
    attempts.clear(budgetKey, "-");
    UUID id = account.user().id();
    accounts.updatePasswordAndRevokeOtherSessions(
        id, passwords.encode(password), command.sessionId());
    audit.record(new AuditEntry(id, "PASSWORD_CHANGED", "USER", id.toString(), null, null));
  }
}
