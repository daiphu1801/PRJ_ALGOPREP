package com.algoprep.identity.application.ports.in;

import com.algoprep.identity.application.dto.VerifyEmailCommand;

/** Email confirmation, endpoint #53. */
public interface VerifyEmailUseCase {
  void verify(VerifyEmailCommand command);
}
