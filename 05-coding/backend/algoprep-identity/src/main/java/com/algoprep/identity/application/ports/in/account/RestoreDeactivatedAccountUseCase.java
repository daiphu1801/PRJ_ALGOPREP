package com.algoprep.identity.application.ports.in.account;

import com.algoprep.identity.application.dto.account.RestoreAccountCommand;

/** Cancels self-deactivation without issuing a session (#61). */
public interface RestoreDeactivatedAccountUseCase {
  void restore(RestoreAccountCommand command);
}
