package com.algoprep.identity.application.command;

import com.algoprep.common.domain.ports.out.IdGeneratorPort;
import com.algoprep.common.exception.DomainRuleViolationException;
import com.algoprep.identity.application.dto.CreateUserAccountCommand;
import com.algoprep.identity.application.ports.out.PasswordHasherPort;
import com.algoprep.identity.domain.exception.IdentityErrorCodes;
import com.algoprep.identity.domain.model.RbacRole;
import com.algoprep.identity.domain.ports.out.RbacRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Validates the input of #50 and prepares the account: normalised e-mail and name, a non-ADMIN
 * role, a new id and a temporary password with its hash.
 */
@Component
class NewAdminAccountFactory {

  /** The account to insert; {@code temporaryPassword} is only for the e-mail, never stored. */
  record NewAccount(
      UUID id,
      String email,
      String displayName,
      RbacRole role,
      String temporaryPassword,
      String passwordHash) {}

  private final RegistrationValidation validation;
  private final RbacRepository rbac;
  private final IdGeneratorPort ids;
  private final PasswordHasherPort hasher;
  private final TemporaryPasswordGenerator generator;

  NewAdminAccountFactory(
      RegistrationValidation validation,
      RbacRepository rbac,
      IdGeneratorPort ids,
      PasswordHasherPort hasher,
      TemporaryPasswordGenerator generator) {
    this.validation = validation;
    this.rbac = rbac;
    this.ids = ids;
    this.hasher = hasher;
    this.generator = generator;
  }

  NewAccount prepare(CreateUserAccountCommand command) {
    String address = validation.email(command.email());
    String name = validation.displayName(command.displayName());
    RbacRole role = role(command.roleId());
    String temporary = generator.generate();
    return new NewAccount(ids.newId(), address, name, role, temporary, hasher.encode(temporary));
  }

  /** Missing, malformed, unknown or ADMIN-category role ids are all {@code 422 IDT-441}. */
  private RbacRole role(String raw) {
    Optional<RbacRole> role = parse(raw).flatMap(rbac::lockRole);
    if (role.isEmpty() || "ADMIN".equals(role.get().baseCategory())) {
      throw new DomainRuleViolationException(
          IdentityErrorCodes.ADMIN_CREATE_ROLE_INVALID,
          "The role cannot be used for a new account");
    }
    return role.get();
  }

  private static Optional<UUID> parse(String raw) {
    try {
      return raw == null ? Optional.empty() : Optional.of(UUID.fromString(raw.trim()));
    } catch (IllegalArgumentException e) {
      return Optional.empty();
    }
  }
}
