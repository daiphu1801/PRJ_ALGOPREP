package com.algoprep.identity.application.ports.in.account;

import com.algoprep.identity.application.dto.account.ConfirmEmailChangeCommand;

/** Confirms the code and moves the login e-mail (#12). */
public interface ConfirmEmailChangeUseCase {

  /**
   * @return the new e-mail now stored
   */
  String confirm(ConfirmEmailChangeCommand command);
}
