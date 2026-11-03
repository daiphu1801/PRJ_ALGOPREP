package com.algoprep.identity.application.ports.in;

import com.algoprep.identity.application.dto.VerifyPasswordResetOtpCommand;

/** Checks the code before the user enters a new password (#60). */
public interface VerifyPasswordResetOtpUseCase {
  void verify(VerifyPasswordResetOtpCommand command);
}
