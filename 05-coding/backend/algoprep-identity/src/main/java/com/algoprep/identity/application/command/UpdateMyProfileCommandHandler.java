package com.algoprep.identity.application.command;

import com.algoprep.common.exception.DomainRuleViolationException;
import com.algoprep.common.exception.UnauthenticatedException;
import com.algoprep.identity.application.dto.UpdateMyProfileCommand;
import com.algoprep.identity.application.ports.in.UpdateMyProfileUseCase;
import com.algoprep.identity.domain.exception.IdentityErrorCodes;
import com.algoprep.identity.domain.model.MyProfile;
import com.algoprep.identity.domain.model.ProfileFields;
import com.algoprep.identity.domain.ports.out.MyProfileRepository;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Merges the sent fields over the stored ones and validates them (DD validation F1 section 5). The
 * row is the caller's own and the id comes from the token, so only one person edits it; two tabs
 * saving different fields at once would be last-write-wins on the whole profile.
 */
@Service
class UpdateMyProfileCommandHandler implements UpdateMyProfileUseCase {
  private static final Pattern PHONE = Pattern.compile("[0-9 +()-]*");
  private final MyProfileRepository profiles;
  private final RegistrationValidation validation;

  UpdateMyProfileCommandHandler(MyProfileRepository profiles, RegistrationValidation validation) {
    this.profiles = profiles;
    this.validation = validation;
  }

  @Override
  @Transactional
  public MyProfile update(UpdateMyProfileCommand command) {
    MyProfile current =
        profiles
            .findById(command.userId())
            .orElseThrow(
                () ->
                    new UnauthenticatedException(
                        IdentityErrorCodes.REFRESH_TOKEN_INVALID, "Account is not available"));
    profiles.updateFields(
        command.userId(),
        new ProfileFields(
            command.displayName() == null
                ? current.displayName()
                : validation.displayName(command.displayName()),
            text(command.schoolOrCompany(), current.schoolOrCompany(), 150, "IDT-V09"),
            text(command.currentPosition(), current.currentPosition(), 100, "IDT-V07"),
            text(command.targetPosition(), current.targetPosition(), 100, "IDT-V08"),
            phone(command.phone(), current.phone())));
    return profiles.findById(command.userId()).orElseThrow();
  }

  /** Not sent keeps the stored value; empty after trimming clears it; otherwise at most max. */
  private static String text(String sent, String stored, int max, String code) {
    if (sent == null) {
      return stored;
    }
    String value = sent.trim();
    if (value.length() > max) {
      throw new DomainRuleViolationException(code, "Value is too long");
    }
    return value.isEmpty() ? null : value;
  }

  private static String phone(String sent, String stored) {
    String value = text(sent, stored, 20, "IDT-V13");
    if (sent != null && value != null && !PHONE.matcher(value).matches()) {
      throw new DomainRuleViolationException("IDT-V13", "Phone number has invalid characters");
    }
    return value;
  }
}
