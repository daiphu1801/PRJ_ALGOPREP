package com.algoprep.identity.application.ports.in.password;

import com.algoprep.identity.application.dto.password.ResetPasswordCommand;

/** Changes password and revokes existing refresh sessions (#8). */
public interface ResetPasswordUseCase {
  void reset(ResetPasswordCommand command);
}
