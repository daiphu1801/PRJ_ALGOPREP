package com.algoprep.identity.application.command;

import com.algoprep.common.exception.InvalidRequestException;
import com.algoprep.common.exception.ServiceUnavailableException;
import com.algoprep.identity.application.dto.OAuthStart;
import com.algoprep.identity.application.ports.in.StartOAuthLoginUseCase;
import com.algoprep.identity.application.ports.out.OAuthProviderPort;
import com.algoprep.identity.application.ports.out.OAuthStatePort;
import com.algoprep.identity.domain.exception.IdentityErrorCodes;
import com.algoprep.identity.domain.model.OAuthProvider;
import java.security.SecureRandom;
import java.util.Base64;
import org.springframework.stereotype.Service;

/** Starts a flow: a random one-time state, remembered for ten minutes, inside the provider URL. */
@Service
class StartOAuthLoginCommandHandler implements StartOAuthLoginUseCase {
  private static final int STATE_BYTES = 32;

  private final OAuthProviderPort providers;
  private final OAuthStatePort states;
  private final SecureRandom random = new SecureRandom();

  StartOAuthLoginCommandHandler(OAuthProviderPort providers, OAuthStatePort states) {
    this.providers = providers;
    this.states = states;
  }

  @Override
  public OAuthStart start(String provider) {
    OAuthProvider selected =
        OAuthProvider.fromPath(provider)
            .orElseThrow(
                () ->
                    new InvalidRequestException(
                        IdentityErrorCodes.OAUTH_PROVIDER_UNKNOWN, "Unknown OAuth provider"));
    byte[] bytes = new byte[STATE_BYTES];
    random.nextBytes(bytes);
    String state = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    String url =
        providers
            .authorizationUrl(selected, state)
            .orElseThrow(
                () ->
                    new ServiceUnavailableException(
                        IdentityErrorCodes.OAUTH_UNAVAILABLE,
                        "OAuth provider is not configured",
                        null));
    states.store(state);
    return new OAuthStart(url, state);
  }
}
