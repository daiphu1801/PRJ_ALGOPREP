package com.algoprep.identity.application.ports.in;

import com.algoprep.identity.application.dto.LoginResult;

/** OAuth callback #6 (DD F1 BR-03 steps 2 to 6); answers like a password login. */
public interface CompleteOAuthLoginUseCase {
  LoginResult complete(String provider, String code, String state);
}
