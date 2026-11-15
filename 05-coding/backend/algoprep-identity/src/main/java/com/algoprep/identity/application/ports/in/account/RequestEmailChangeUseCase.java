package com.algoprep.identity.application.ports.in;

import com.algoprep.identity.application.dto.PendingEmailChange;
import com.algoprep.identity.application.dto.RequestEmailChangeCommand;

/** Sends a code to a new address without changing the login e-mail yet (#11). */
public interface RequestEmailChangeUseCase {
  PendingEmailChange request(RequestEmailChangeCommand command);
}
