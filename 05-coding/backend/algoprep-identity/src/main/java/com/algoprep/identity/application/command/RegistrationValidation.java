package com.algoprep.identity.application.command;

import com.algoprep.common.exception.DomainRuleViolationException;
import com.algoprep.common.exception.InvalidRequestException;
import com.algoprep.identity.application.dto.RegisterCommand;
import com.algoprep.identity.domain.model.PasswordPolicy;
import jakarta.validation.Validator;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Locale;
import org.springframework.stereotype.Component;

/** Validation from DD F1 sections 1, 3 and 5; messages never echo input values. */
@Component
class RegistrationValidation {
  private final Validator validator;

  RegistrationValidation(Validator validator) {
    this.validator = validator;
  }

  private record EmailInput(@NotBlank @Email @Size(max = 254) String email) {}

  String email(String value) {
    String normalized = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    if (!validator.validate(new EmailInput(normalized)).isEmpty()) {
      throw new InvalidRequestException("IDT-400", "A valid email is required");
    }
    return normalized;
  }

  RegisterCommand registration(RegisterCommand input) {
    String email = email(input.email());
    String name = displayName(input.displayName());
    String password = password(input.password(), email, name);
    return new RegisterCommand(email, password, name);
  }

  /** Trimmed 2 to 100 characters, shared by registration and profile edit. */
  String displayName(String value) {
    String name = value == null ? "" : value.trim();
    require(name.length() >= 2, "IDT-V19", "Display name is too short");
    require(name.length() <= 100, "IDT-V20", "Display name is too long");
    return name;
  }

  String password(String value, String email, String displayName) {
    String password = value == null ? "" : value;
    require(password.length() >= 8, "IDT-V01", "Password is too short");
    require(PasswordPolicy.fitsBcrypt(password), "IDT-V02", "Password exceeds 72 UTF-8 bytes");
    require(
        password.codePoints().anyMatch(Character::isLetter)
            && password.codePoints().anyMatch(Character::isDigit),
        "IDT-V03",
        "Password needs a letter and a digit");
    require(
        !password.equalsIgnoreCase(email) && !password.equalsIgnoreCase(displayName),
        "IDT-V04",
        "Password must differ from email and display name");
    return password;
  }

  private static void require(boolean condition, String code, String message) {
    if (!condition) {
      throw new DomainRuleViolationException(code, message);
    }
  }
}
