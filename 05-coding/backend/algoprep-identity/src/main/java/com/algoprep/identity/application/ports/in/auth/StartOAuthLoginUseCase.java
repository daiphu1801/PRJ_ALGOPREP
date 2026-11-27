package com.algoprep.identity.application.ports.in.auth;

import com.algoprep.identity.application.dto.auth.OAuthStart;

/** OAuth redirect #5 (DD F1 BR-03 step 1); {@code provider} is the path segment. */
public interface StartOAuthLoginUseCase {
  OAuthStart start(String provider);
}
