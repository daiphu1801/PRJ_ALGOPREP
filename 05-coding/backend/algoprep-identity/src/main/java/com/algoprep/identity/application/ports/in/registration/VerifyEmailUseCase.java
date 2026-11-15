package com.algoprep.identity.application.ports.in.registration;

import com.algoprep.identity.application.dto.registration.VerifyEmailCommand;

/** Email confirmation, endpoint #53. */
public interface VerifyEmailUseCase {
  void verify(VerifyEmailCommand command);
}
