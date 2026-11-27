package com.algoprep.identity.application.ports.in.registration;

import com.algoprep.identity.application.dto.registration.RegisterCommand;
import com.algoprep.identity.application.dto.registration.RegistrationResponse;

/** Self-registration, endpoint #1. */
public interface RegisterUseCase {
  RegistrationResponse register(RegisterCommand command);
}
