package com.algoprep.identity.application.command;

import com.algoprep.identity.application.dto.ResetPasswordCommand;
import com.algoprep.identity.application.ports.in.ResetPasswordUseCase;
import com.algoprep.identity.application.ports.out.PasswordHasherPort;
import com.algoprep.identity.domain.model.AuditEntry;
import com.algoprep.identity.domain.model.PasswordResetAccount;
import com.algoprep.identity.domain.ports.out.AuditLogPort;
import com.algoprep.identity.domain.ports.out.PasswordResetRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Atomically changes the credential, revokes refresh sessions and records the reset (BR-04). */
@Service
class ResetPasswordCommandHandler implements ResetPasswordUseCase {
  private final PasswordResetRepository accounts;
  private final PasswordResetService reset;
  private final PasswordHasherPort passwords;
  private final RegistrationValidation validation;
  private final AuditLogPort audit;

  ResetPasswordCommandHandler(
      PasswordResetRepository accounts,
      PasswordResetService reset,
      PasswordHasherPort passwords,
      RegistrationValidation validation,
      AuditLogPort audit) {
    this.accounts = accounts;
    this.reset = reset;
    this.passwords = passwords;
    this.validation = validation;
    this.audit = audit;
  }

  @Override
  @Transactional
  public void reset(ResetPasswordCommand command) {
    String email = validation.email(command.email());
    Optional<PasswordResetAccount> found = accounts.lockByEmail(email);
    Instant issued = reset.check(found.orElse(null), command.otp());
    PasswordResetAccount account = found.orElseThrow();
    String password =
        validation.password(command.newPassword(), email, account.user().displayName());
    UUID id = account.user().id();
    accounts.updatePasswordAndRevokeSessions(id, passwords.encode(password));
    audit.record(new AuditEntry(id, "PASSWORD_RESET_COMPLETED", "USER", id.toString(), null, null));
    reset.clearAfterCommit(id, issued);
  }
}
