package com.algoprep.identity.domain.ports.out;

import com.algoprep.identity.domain.model.OAuthProvider;
import java.util.Optional;
import java.util.UUID;

/**
 * Accounts and provider links touched by the OAuth callback (DD F1 BR-03); joins the caller's
 * transaction.
 */
public interface OAuthAccountRepository {

  /** The user already linked to this provider account. */
  Optional<UUID> findLinkedUser(OAuthProvider provider, String providerUserId);

  /**
   * Locks and returns the user holding {@code email}, so a concurrent callback for the same address
   * waits instead of creating a second account.
   */
  Optional<UUID> lockUserByEmail(String email);

  /**
   * A new ACTIVE student without a password (BR-03 step 5); empty when the e-mail just appeared.
   */
  Optional<UUID> createStudent(String email, String displayName);

  /** Records the link; false when this provider account is already linked (a lost race). */
  boolean link(UUID userId, OAuthProvider provider, String providerUserId, String providerEmail);

  /**
   * Drops the password of a still-unverified self-registered account and activates it: whoever
   * typed that password never proved they own the address, the provider just did.
   */
  void claimPendingAccount(UUID userId);
}
