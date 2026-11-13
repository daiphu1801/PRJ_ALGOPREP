package com.algoprep.identity.application.command;

import com.algoprep.common.exception.DomainRuleViolationException;
import com.algoprep.identity.application.dto.VerifyEmailCommand;
import com.algoprep.identity.application.ports.in.VerifyEmailUseCase;
import com.algoprep.identity.domain.model.UserAccount;
import com.algoprep.identity.domain.model.UserStatus;
import com.algoprep.identity.domain.ports.out.RegistrationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Row lock makes activation one-time; Redis code is removed only after DB commit. */
@Service
public class VerifyEmailCommandHandler implements VerifyEmailUseCase {
  private final RegistrationRepository accounts;
  private final EmailVerificationService verification;
  private final RegistrationValidation validation;

  public VerifyEmailCommandHandler(
      RegistrationRepository accounts,
      EmailVerificationService verification,
      RegistrationValidation validation) {
    this.accounts = accounts;
    this.verification = verification;
    this.validation = validation;
  }

  @Override
  @Transactional
  public void verify(VerifyEmailCommand command) {
    String email = validation.email(command.email());
    if (command.otp() == null || !command.otp().matches("[0-9]{6}")) {
      throw invalidCode();
    }
    UserAccount user =
        accounts.lockByEmail(email).orElseThrow(VerifyEmailCommandHandler::invalidCode);
    if (user.status() == UserStatus.ACTIVE) {
      return;
    }
    if (user.status() != UserStatus.PENDING_VERIFICATION
        || user.mustChangePassword()
        || !verification.matches(user.id(), command.otp())) {
      throw invalidCode();
    }
    accounts.activate(user.id());
    verification.clearAfterCommit(user.id(), command.otp());
  }

  private static DomainRuleViolationException invalidCode() {
    return new DomainRuleViolationException(
        "IDT-112", "Verification code is invalid or unavailable");
  }
}
