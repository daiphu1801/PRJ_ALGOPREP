package com.algoprep.identity.application.ports.in;

import com.algoprep.identity.application.dto.RegisterCommand;
import com.algoprep.identity.application.dto.RegistrationResponse;

/** Self-registration, endpoint #1. */
public interface RegisterUseCase {
  RegistrationResponse register(RegisterCommand command);
}
