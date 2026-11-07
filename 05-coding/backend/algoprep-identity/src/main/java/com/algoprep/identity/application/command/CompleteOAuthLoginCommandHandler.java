package com.algoprep.identity.application.command;

import com.algoprep.common.exception.InvalidRequestException;
import com.algoprep.identity.application.dto.LoginResult;
import com.algoprep.identity.application.dto.UserSummary;
import com.algoprep.identity.application.ports.in.CompleteOAuthLoginUseCase;
import com.algoprep.identity.domain.exception.IdentityErrorCodes;
import com.algoprep.identity.domain.model.OAuthProfile;
import com.algoprep.identity.domain.model.OAuthProvider;
import com.algoprep.identity.domain.model.UserAccount;
import com.algoprep.identity.domain.model.UserStatus;
import com.algoprep.identity.domain.ports.out.OAuthAccountRepository;
import com.algoprep.identity.domain.ports.out.UserAccountRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * DD F1 BR-03 steps 5 and 6: link by verified e-mail or create a student, then open a session like
 * a password login. A self-deactivated account inside its grace period gets only a restore ticket
 * (through {@code admitByStatus}), never a session.
 */
@Service
class CompleteOAuthLoginCommandHandler implements CompleteOAuthLoginUseCase {
  private static final int NAME_MAX = 100;

  private final OAuthExchangeService exchange;
  private final OAuthAccountRepository accounts;
  private final UserAccountRepository users;
  private final LoginAccountService admission;
  private final SessionIssuer sessions;

  CompleteOAuthLoginCommandHandler(
      OAuthExchangeService exchange,
      OAuthAccountRepository accounts,
      UserAccountRepository users,
      LoginAccountService admission,
      SessionIssuer sessions) {
    this.exchange = exchange;
    this.accounts = accounts;
    this.users = users;
    this.admission = admission;
    this.sessions = sessions;
  }

  @Override
  @Transactional
  public LoginResult complete(String provider, String code, String state) {
    OAuthProvider selected =
        OAuthProvider.fromPath(provider)
            .orElseThrow(
                () ->
                    new InvalidRequestException(
                        IdentityErrorCodes.OAUTH_PROVIDER_UNKNOWN, "Unknown OAuth provider"));
    OAuthProfile profile = exchange.verify(selected, code, state);
    UUID userId =
        accounts
            .findLinkedUser(selected, profile.providerUserId())
            .orElseGet(() -> linkOrCreate(selected, profile));
    UserAccount user =
        users.findById(userId).orElseThrow(() -> new IllegalStateException("Linked user missing"));
    admission.admitByStatus(user);
    SessionIssuer.Session session = sessions.openNewFamily(user);
    admission.touchLastActive(user.id());
    UserSummary summary =
        new UserSummary(
            user.id(), user.displayName(), user.baseCategory(), user.mustChangePassword());
    return new LoginResult(
        session.accessToken().value(),
        session.expiresInSeconds(),
        summary,
        session.rawRefreshToken());
  }

  private UUID linkOrCreate(OAuthProvider provider, OAuthProfile profile) {
    String email = profile.email().toLowerCase(java.util.Locale.ROOT);
    UUID id =
        accounts
            .lockUserByEmail(email)
            .map(existing -> claimIfPending(existing))
            .orElseGet(
                () ->
                    accounts
                        .createStudent(email, nameOf(profile, email))
                        .orElseGet(
                            () ->
                                accounts
                                    .lockUserByEmail(email)
                                    .orElseThrow(
                                        () -> new IllegalStateException("Account vanished"))));
    if (!accounts.link(id, provider, profile.providerUserId(), email)) {
      return accounts
          .findLinkedUser(provider, profile.providerUserId())
          .orElseThrow(() -> new IllegalStateException("Link lost"));
    }
    return id;
  }

  /** A self-registered account nobody has verified becomes the OAuth user's, minus its password. */
  private UUID claimIfPending(UUID existing) {
    users
        .findById(existing)
        .filter(u -> u.status() == UserStatus.PENDING_VERIFICATION && !u.mustChangePassword())
        .ifPresent(u -> accounts.claimPendingAccount(u.id()));
    return existing;
  }

  private static String nameOf(OAuthProfile profile, String email) {
    String name = profile.displayName() == null ? "" : profile.displayName().trim();
    if (name.length() < 2) {
      name = email.substring(0, email.indexOf('@'));
    }
    return name.length() > NAME_MAX ? name.substring(0, NAME_MAX) : name;
  }
}
