package com.algoprep.identity.application.command;

import com.algoprep.common.domain.ports.out.IdGeneratorPort;
import com.algoprep.common.exception.ResourceConflictException;
import com.algoprep.identity.application.dto.RegisterCommand;
import com.algoprep.identity.application.dto.RegistrationResponse;
import com.algoprep.identity.application.ports.in.RegisterUseCase;
import com.algoprep.identity.application.ports.out.PasswordHasherPort;
import com.algoprep.identity.domain.model.RegistrationAccount;
import com.algoprep.identity.domain.ports.out.RegistrationRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Creates only a pending STUDENT and sends the initial verification code (BR-21). */
@Service
public class RegisterCommandHandler implements RegisterUseCase {
  private final RegistrationRepository accounts;
  private final PasswordHasherPort passwords;
  private final IdGeneratorPort ids;
  private final EmailVerificationService verification;
  private final RegistrationValidation validation;

  public RegisterCommandHandler(
      RegistrationRepository accounts,
      PasswordHasherPort passwords,
      IdGeneratorPort ids,
      EmailVerificationService verification,
      RegistrationValidation validation) {
    this.accounts = accounts;
    this.passwords = passwords;
    this.ids = ids;
    this.verification = verification;
    this.validation = validation;
  }

  @Override
  @Transactional
  public RegistrationResponse register(RegisterCommand command) {
    RegisterCommand input = validation.registration(command);
    UUID id = ids.newId();
    if (!accounts.create(
        new RegistrationAccount(
            id, input.email(), passwords.encode(input.password()), input.displayName()))) {
      throw new ResourceConflictException("IDT-101", "Email already registered");
    }
    verification.send(id, input.email());
    return new RegistrationResponse(id, input.email());
  }
}
