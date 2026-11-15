package com.algoprep.identity.application.command;

import com.algoprep.common.exception.PermissionDeniedException;
import com.algoprep.common.exception.RateLimitExceededException;
import com.algoprep.common.exception.UnauthenticatedException;
import com.algoprep.identity.application.dto.LoginCommand;
import com.algoprep.identity.application.dto.LoginResult;
import com.algoprep.identity.application.dto.UserSummary;
import com.algoprep.identity.application.ports.in.LoginUseCase;
import com.algoprep.identity.application.ports.out.LoginAttemptPort;
import com.algoprep.identity.application.ports.out.PasswordHasherPort;
import com.algoprep.identity.domain.exception.IdentityErrorCodes;
import com.algoprep.identity.domain.model.BaseCategory;
import com.algoprep.identity.domain.model.UserAccount;
import java.util.Locale;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Login for endpoints #2 and #49 (DD BR-21, validation section 7).
 *
 * <p>Order matters. The password is checked first, and every failure at that step is the same
 * {@code IDT-102}, so a caller learns nothing about whether the email exists. Status and portal
 * checks run only after the password matched, so they reveal nothing to someone who does not know
 * the password. BR-01 checks Redis before looking up the account. An eligible self-deactivated
 * account receives only a restore ticket in {@code IDT-104}, never a login session.
 */
@Service
class LoginCommandHandler implements LoginUseCase {

  private final LoginAccountService users;
  private final PasswordHasherPort hasher;
  private final SessionIssuer sessions;
  private final LoginAttemptPort attempts;

  LoginCommandHandler(
      LoginAccountService users,
      PasswordHasherPort hasher,
      SessionIssuer sessions,
      LoginAttemptPort attempts) {
    this.users = users;
    this.hasher = hasher;
    this.sessions = sessions;
    this.attempts = attempts;
  }

  @Override
  @Transactional
  public LoginResult login(LoginCommand command) {
    String email = command.email() == null ? "" : command.email().trim().toLowerCase(Locale.ROOT);
    String ip = command.sourceIp();
    if (attempts.isBlocked(email, ip)) {
      throw new RateLimitExceededException(
          IdentityErrorCodes.LOGIN_RATE_LIMITED, "Too many failed login attempts");
    }
    String password = command.password() == null ? "" : command.password();
    Optional<UserAccount> found = email.isEmpty() ? Optional.empty() : users.findByEmail(email);

    boolean passwordOk =
        hasher.matches(password, found.map(UserAccount::passwordHash).orElse(null));
    if (!passwordOk) {
      attempts.recordFailure(email, ip);
      throw new UnauthenticatedException(
          IdentityErrorCodes.INVALID_CREDENTIALS, "Wrong email or password");
    }
    UserAccount user = found.orElseThrow();

    users.admitByStatus(user);
    if (command.instructorPortal() && user.baseCategory() != BaseCategory.INSTRUCTOR) {
      throw new PermissionDeniedException(
          IdentityErrorCodes.NOT_AN_INSTRUCTOR, "Account is not an instructor");
    }

    SessionIssuer.Session session = sessions.openNewFamily(user);
    users.touchLastActive(user.id());
    attempts.clear(email, ip);
    UserSummary summary =
        new UserSummary(
            user.id(), user.displayName(), user.baseCategory(), user.mustChangePassword());
    return new LoginResult(
        session.accessToken().value(),
        session.expiresInSeconds(),
        summary,
        session.rawRefreshToken());
  }
}
