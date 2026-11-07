package com.algoprep.identity.application.command;

import com.algoprep.common.exception.DomainRuleViolationException;
import com.algoprep.common.exception.ServiceUnavailableException;
import com.algoprep.common.exception.UnauthenticatedException;
import com.algoprep.identity.application.ports.out.OAuthProviderPort;
import com.algoprep.identity.application.ports.out.OAuthStatePort;
import com.algoprep.identity.domain.exception.IdentityErrorCodes;
import com.algoprep.identity.domain.exception.OAuthCodeRejectedException;
import com.algoprep.identity.domain.model.OAuthProfile;
import com.algoprep.identity.domain.model.OAuthProvider;
import org.springframework.stereotype.Component;

/**
 * Steps 2 to 4 of BR-03: the state is single-use, the code is exchanged, and a profile whose e-mail
 * the provider did not verify is refused before any account is touched.
 */
@Component
class OAuthExchangeService {
  private final OAuthProviderPort providers;
  private final OAuthStatePort states;

  OAuthExchangeService(OAuthProviderPort providers, OAuthStatePort states) {
    this.providers = providers;
    this.states = states;
  }

  OAuthProfile verify(OAuthProvider provider, String code, String state) {
    if (state == null || state.isBlank() || code == null || code.isBlank()) {
      throw stateInvalid();
    }
    if (!states.consume(state)) {
      throw stateInvalid();
    }
    OAuthProfile profile;
    try {
      profile = providers.fetchProfile(provider, code);
    } catch (OAuthCodeRejectedException rejected) {
      throw stateInvalid();
    } catch (RuntimeException failure) {
      // The cause is dropped on purpose: provider errors can echo the code or the address.
      throw new ServiceUnavailableException(
          IdentityErrorCodes.OAUTH_UNAVAILABLE, "OAuth provider is unavailable", null);
    }
    if (!profile.emailVerified() || profile.email() == null || profile.email().isBlank()) {
      throw new DomainRuleViolationException(
          IdentityErrorCodes.OAUTH_EMAIL_UNVERIFIED, "The provider did not verify the e-mail");
    }
    return profile;
  }

  private static UnauthenticatedException stateInvalid() {
    return new UnauthenticatedException(
        IdentityErrorCodes.OAUTH_STATE_INVALID, "OAuth state is not valid");
  }
}
