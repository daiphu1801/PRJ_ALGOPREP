package com.algoprep.identity.application.command;

import com.algoprep.identity.application.dto.VerifyPasswordResetOtpCommand;
import com.algoprep.identity.application.ports.in.VerifyPasswordResetOtpUseCase;
import com.algoprep.identity.domain.ports.out.PasswordResetRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Keeps OTP and its original TTL on success, sharing the wrong-attempt budget with reset. */
@Service
class VerifyPasswordResetOtpCommandHandler implements VerifyPasswordResetOtpUseCase {
  private final PasswordResetRepository accounts;
  private final PasswordResetService reset;
  private final RegistrationValidation validation;

  VerifyPasswordResetOtpCommandHandler(
      PasswordResetRepository accounts,
      PasswordResetService reset,
      RegistrationValidation validation) {
    this.accounts = accounts;
    this.reset = reset;
    this.validation = validation;
  }

  @Override
  @Transactional
  public void verify(VerifyPasswordResetOtpCommand command) {
    reset.check(
        accounts.lockByEmail(validation.email(command.email())).orElse(null), command.otp());
  }
}
