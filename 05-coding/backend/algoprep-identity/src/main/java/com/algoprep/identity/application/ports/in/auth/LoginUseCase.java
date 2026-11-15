package com.algoprep.identity.application.ports.in.auth;

import com.algoprep.identity.application.dto.auth.LoginCommand;
import com.algoprep.identity.application.dto.auth.LoginResult;

/** Endpoints #2 and #49: log in with email and password. */
public interface LoginUseCase {

  /**
   * @param command credentials and which portal was used
   * @return access token, refresh token value and user summary
   */
  LoginResult login(LoginCommand command);
}
