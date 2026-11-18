package com.algoprep.identity.domain.model.auth;

/**
 * What the provider vouches for after a successful code exchange. {@code email} is lower-case and
 * only meaningful when {@code emailVerified} is true. {@code pictureUrl} is the avatar URL from the
 * provider's own userinfo answer (Google {@code picture}, GitHub {@code avatar_url}), or null; it
 * is untrusted until the avatar importer has checked it (BD security F1 section 6.5).
 */
public record OAuthProfile(
    OAuthProvider provider,
    String providerUserId,
    String email,
    boolean emailVerified,
    String displayName,
    String pictureUrl) {

  @Override
  public String toString() {
    return "OAuthProfile[" + provider + "]";
  }
}
