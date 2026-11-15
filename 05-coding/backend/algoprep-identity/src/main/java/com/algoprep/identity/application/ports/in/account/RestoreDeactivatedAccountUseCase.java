package com.algoprep.identity.application.ports.in;

import com.algoprep.identity.application.dto.RestoreAccountCommand;

/** Cancels self-deactivation without issuing a session (#61). */
public interface RestoreDeactivatedAccountUseCase {
  void restore(RestoreAccountCommand command);
}
