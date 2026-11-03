package com.algoprep.identity.application.ports.in;

import com.algoprep.identity.application.dto.ChangeMyPasswordCommand;

/** Changes the caller's password and revokes their other sessions (#13, BR-06). */
public interface ChangeMyPasswordUseCase {
  void change(ChangeMyPasswordCommand command);
}
