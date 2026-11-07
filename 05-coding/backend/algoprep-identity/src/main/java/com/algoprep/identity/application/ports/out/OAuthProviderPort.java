package com.algoprep.identity.application.ports.out;

import com.algoprep.identity.domain.model.OAuthProfile;
import com.algoprep.identity.domain.model.OAuthProvider;
import java.util.Optional;

/**
 * The provider's side of the flow. Failures of the provider are unchecked exceptions; a code the
 * provider refuses is {@link OAuthCodeRejectedException}, anything else is "unavailable".
 */
public interface OAuthProviderPort {

  /** The authorization URL carrying {@code state}, or empty when the provider is not configured. */
  Optional<String> authorizationUrl(OAuthProvider provider, String state);

  /** Exchanges the one-time {@code code} and reads the signed-in user's profile. */
  OAuthProfile fetchProfile(OAuthProvider provider, String code);
}
