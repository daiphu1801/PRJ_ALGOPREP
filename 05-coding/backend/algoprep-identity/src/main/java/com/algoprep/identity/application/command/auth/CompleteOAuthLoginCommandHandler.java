package com.algoprep.identity.application.command.auth;

import com.algoprep.common.exception.InvalidRequestException;
import com.algoprep.identity.application.dto.auth.LoginResult;
import com.algoprep.identity.application.ports.in.auth.CompleteOAuthLoginUseCase;
import com.algoprep.identity.domain.exception.IdentityErrorCodes;
import com.algoprep.identity.domain.model.UserAccount;
import com.algoprep.identity.domain.model.UserStatus;
import com.algoprep.identity.domain.model.auth.OAuthProfile;
import com.algoprep.identity.domain.model.auth.OAuthProvider;
import com.algoprep.identity.domain.ports.out.OAuthAccountRepository;
import com.algoprep.identity.domain.ports.out.UserAccountRepository;
import java.util.Optional;
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
  private final OAuthLoginFinisher finisher;

  CompleteOAuthLoginCommandHandler(
      OAuthExchangeService exchange,
      OAuthAccountRepository accounts,
      UserAccountRepository users,
      OAuthLoginFinisher finisher) {
    this.exchange = exchange;
    this.accounts = accounts;
    this.users = users;
    this.finisher = finisher;
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
    Optional<UUID> linked = accounts.findLinkedUser(selected, profile.providerUserId());
    UUID userId = linked.orElseGet(() -> linkOrCreate(selected, profile));
    UserAccount user =
        users.findById(userId).orElseThrow(() -> new IllegalStateException("Linked user missing"));
    return finisher.finish(user, profile, linked.isEmpty());
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
