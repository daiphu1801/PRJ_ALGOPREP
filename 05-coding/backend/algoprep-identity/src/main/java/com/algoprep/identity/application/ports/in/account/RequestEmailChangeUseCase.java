package com.algoprep.identity.application.ports.in.account;

import com.algoprep.identity.application.dto.account.PendingEmailChange;
import com.algoprep.identity.application.dto.account.RequestEmailChangeCommand;

/** Sends a code to a new address without changing the login e-mail yet (#11). */
public interface RequestEmailChangeUseCase {
  PendingEmailChange request(RequestEmailChangeCommand command);
}
