package com.algoprep.identity.domain.model;

/**
 * What the provider vouches for after a successful code exchange. {@code email} is lower-case and
 * only meaningful when {@code emailVerified} is true.
 */
public record OAuthProfile(
    OAuthProvider provider,
    String providerUserId,
    String email,
    boolean emailVerified,
    String displayName) {

  @Override
  public String toString() {
    return "OAuthProfile[" + provider + "]";
  }
}
