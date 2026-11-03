package com.algoprep.identity.application.command;

import com.algoprep.common.domain.ports.out.IdGeneratorPort;
import com.algoprep.common.exception.DomainRuleViolationException;
import com.algoprep.common.exception.InvalidRequestException;
import com.algoprep.identity.application.ports.in.BootstrapFirstAdminUseCase;
import com.algoprep.identity.application.ports.out.PasswordHasherPort;
import com.algoprep.identity.domain.ports.out.AdminBootstrapRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * DD BR-23 / JOB-ID-01: seeds the first ADMIN. Idempotent: once any ADMIN exists the configuration
 * is not read, validated or logged. Never writes an audit row (no actor) and never logs the
 * password.
 */
@Service
public class BootstrapFirstAdminCommandHandler implements BootstrapFirstAdminUseCase {
  static final String EMAIL_VARIABLE = "ALGOPREP_BOOTSTRAP_ADMIN_EMAIL";
  static final String PASSWORD_VARIABLE = "ALGOPREP_BOOTSTRAP_ADMIN_PASSWORD";
  // ponytail: fixed display name; the DD gives none and the admin edits it in the profile screen.
  static final String DISPLAY_NAME = "Administrator";

  private static final Logger LOG =
      LoggerFactory.getLogger(BootstrapFirstAdminCommandHandler.class);

  private final AdminBootstrapRepository admins;
  private final PasswordHasherPort passwords;
  private final IdGeneratorPort ids;
  private final RegistrationValidation validation;

  public BootstrapFirstAdminCommandHandler(
      AdminBootstrapRepository admins,
      PasswordHasherPort passwords,
      IdGeneratorPort ids,
      RegistrationValidation validation) {
    this.admins = admins;
    this.passwords = passwords;
    this.ids = ids;
    this.validation = validation;
  }

  @Override
  @Transactional
  public void bootstrap(String email, String password) {
    if (admins.adminExists()) {
      return;
    }
    require(email, EMAIL_VARIABLE);
    require(password, PASSWORD_VARIABLE);
    String normalized = checked(EMAIL_VARIABLE, () -> validation.email(email));
    checked(PASSWORD_VARIABLE, () -> validation.password(password, normalized, DISPLAY_NAME));
    boolean created =
        admins.createAdmin(ids.newId(), normalized, passwords.encode(password), DISPLAY_NAME);
    if (created) {
      LOG.info("First ADMIN account created from {}", EMAIL_VARIABLE);
    } else if (!admins.adminExists()) {
      // Email belongs to a non-admin user. A concurrent instance that created the ADMIN is fine.
      throw new IllegalStateException(
          EMAIL_VARIABLE + " is already used by a non-ADMIN account; no ADMIN exists");
    }
  }

  private static void require(String value, String variable) {
    if (value == null || value.isBlank()) {
      throw new IllegalStateException(
          variable + " is required because no ADMIN account exists yet");
    }
  }

  private static String checked(String variable, java.util.function.Supplier<String> rule) {
    try {
      return rule.get();
    } catch (DomainRuleViolationException | InvalidRequestException e) {
      // The rule message never echoes the value, so it is safe to surface.
      throw new IllegalStateException(variable + " is invalid: " + e.getMessage(), e);
    }
  }
}
