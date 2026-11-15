package com.algoprep.identity.application.command;

import com.algoprep.identity.application.ports.in.ForgotPasswordUseCase;
import com.algoprep.identity.domain.ports.out.PasswordResetRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Unknown emails have no mail, OTP or audit side effect. Cooldown is a silent no-op. */
@Service
class ForgotPasswordCommandHandler implements ForgotPasswordUseCase {
  private final PasswordResetRepository accounts;
  private final PasswordResetService reset;
  private final RegistrationValidation validation;

  ForgotPasswordCommandHandler(
      PasswordResetRepository accounts,
      PasswordResetService reset,
      RegistrationValidation validation) {
    this.accounts = accounts;
    this.reset = reset;
    this.validation = validation;
  }

  @Override
  @Transactional
  public void forgot(String email) {
    accounts.lockByEmail(validation.email(email)).ifPresent(reset::send);
  }
}
