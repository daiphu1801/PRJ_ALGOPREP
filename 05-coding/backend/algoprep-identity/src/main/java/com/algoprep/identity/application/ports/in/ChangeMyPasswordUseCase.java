package com.algoprep.identity.application.ports.in;

import com.algoprep.identity.application.dto.ChangeMyPasswordCommand;

/** Changes the caller's password and revokes their other sessions (#13, BR-06). */
public interface ChangeMyPasswordUseCase {
  /**
   * Returns true when the first-password notice e-mail could not be sent (the change still stands).
   */
  boolean change(ChangeMyPasswordCommand command);
}
