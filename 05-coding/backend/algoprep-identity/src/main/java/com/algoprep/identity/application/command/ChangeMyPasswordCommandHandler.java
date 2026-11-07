package com.algoprep.identity.application.command;

import com.algoprep.common.exception.DomainRuleViolationException;
import com.algoprep.common.exception.RateLimitExceededException;
import com.algoprep.common.exception.UnauthenticatedException;
import com.algoprep.identity.application.dto.ChangeMyPasswordCommand;
import com.algoprep.identity.application.ports.in.ChangeMyPasswordUseCase;
import com.algoprep.identity.application.ports.out.LoginAttemptPort;
import com.algoprep.identity.application.ports.out.PasswordHasherPort;
import com.algoprep.identity.domain.exception.IdentityErrorCodes;
import com.algoprep.identity.domain.model.PasswordChangeAccount;
import com.algoprep.identity.domain.ports.out.PasswordChangeRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Change password (BR-06): new password rules, then the current password, then hash + revoke every
 * session except the caller's. An account with no password yet (OAuth-only, {@code password_hash IS
 * NULL}) sets its first one without a current password (BR-05); a current password sent in that
 * case is ignored, and the same rules, hashing and session revocation apply. The notice e-mail is
 * best effort: when it fails the password still changes and the result says so.
 */
@Service
class ChangeMyPasswordCommandHandler implements ChangeMyPasswordUseCase {
  private final PasswordChangeRepository accounts;
  private final PasswordHasherPort passwords;
  private final RegistrationValidation validation;
  private final LoginAttemptPort attempts;
  private final PasswordChangeRecorder recorder;

  ChangeMyPasswordCommandHandler(
      PasswordChangeRepository accounts,
      PasswordHasherPort passwords,
      RegistrationValidation validation,
      LoginAttemptPort attempts,
      PasswordChangeRecorder recorder) {
    this.accounts = accounts;
    this.passwords = passwords;
    this.validation = validation;
    this.attempts = attempts;
    this.recorder = recorder;
  }

  @Override
  @Transactional
  public boolean change(ChangeMyPasswordCommand command) {
    PasswordChangeAccount account =
        accounts
            .lockById(command.userId())
            .orElseThrow(
                () ->
                    new UnauthenticatedException(
                        IdentityErrorCodes.REFRESH_TOKEN_INVALID, "Account is not available"));
    String password =
        validation.password(command.newPassword(), account.email(), account.user().displayName());
    boolean firstPassword = account.user().passwordHash() == null;
    if (!firstPassword) {
      // A stolen access token must not be able to guess the current password: the same
      // five-in-15-minutes budget as the login (BR-01), keyed by account instead of by e-mail.
      String budgetKey = "pwchange:" + command.userId();
      if (attempts.isBlocked(budgetKey, "-")) {
        throw new RateLimitExceededException(
            IdentityErrorCodes.LOGIN_RATE_LIMITED, "Too many failed attempts");
      }
      String current = command.currentPassword() == null ? "" : command.currentPassword();
      if (!passwords.matches(current, account.user().passwordHash())) {
        attempts.recordFailure(budgetKey, "-");
        throw new DomainRuleViolationException(
            IdentityErrorCodes.CURRENT_PASSWORD_INCORRECT, "Current password is incorrect");
      }
      attempts.clear(budgetKey, "-");
    }
    UUID id = account.user().id();
    accounts.updatePasswordAndRevokeOtherSessions(
        id, passwords.encode(password), command.sessionId());
    return recorder.record(id, account.email(), firstPassword);
  }
}
