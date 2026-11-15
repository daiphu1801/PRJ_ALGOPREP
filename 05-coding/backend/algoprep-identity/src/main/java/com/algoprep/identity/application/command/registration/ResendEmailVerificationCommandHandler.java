package com.algoprep.identity.application.command;

import com.algoprep.identity.application.ports.in.ResendEmailVerificationUseCase;
import com.algoprep.identity.domain.model.UserStatus;
import com.algoprep.identity.domain.ports.out.RegistrationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Unknown, active, deactivated and Admin-created accounts receive no OTP. */
@Service
public class ResendEmailVerificationCommandHandler implements ResendEmailVerificationUseCase {
  private final RegistrationRepository accounts;
  private final EmailVerificationService verification;
  private final RegistrationValidation validation;

  public ResendEmailVerificationCommandHandler(
      RegistrationRepository accounts,
      EmailVerificationService verification,
      RegistrationValidation validation) {
    this.accounts = accounts;
    this.verification = verification;
    this.validation = validation;
  }

  @Override
  @Transactional
  public void resend(String email) {
    String normalized = validation.email(email);
    accounts
        .lockByEmail(normalized)
        .filter(
            user -> user.status() == UserStatus.PENDING_VERIFICATION && !user.mustChangePassword())
        .ifPresent(user -> verification.send(user.id(), normalized));
  }
}
