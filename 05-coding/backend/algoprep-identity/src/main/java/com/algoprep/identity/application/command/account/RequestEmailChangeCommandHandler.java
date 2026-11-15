package com.algoprep.identity.application.command;

import com.algoprep.common.exception.ResourceConflictException;
import com.algoprep.common.exception.UnauthenticatedException;
import com.algoprep.identity.application.dto.PendingEmailChange;
import com.algoprep.identity.application.dto.RequestEmailChangeCommand;
import com.algoprep.identity.application.ports.in.RequestEmailChangeUseCase;
import com.algoprep.identity.domain.exception.IdentityErrorCodes;
import com.algoprep.identity.domain.model.MyProfile;
import com.algoprep.identity.domain.ports.out.MyProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * #11: the address is checked first, so a taken one is refused before any code is issued or mailed
 * (the caller is logged in, so this is not an enumeration risk). The login e-mail itself does not
 * change until #12.
 */
@Service
class RequestEmailChangeCommandHandler implements RequestEmailChangeUseCase {
  static final int CODE_TTL_SECONDS = 600;
  private final MyProfileRepository profiles;
  private final RegistrationValidation validation;
  private final EmailChangeService change;

  RequestEmailChangeCommandHandler(
      MyProfileRepository profiles, RegistrationValidation validation, EmailChangeService change) {
    this.profiles = profiles;
    this.validation = validation;
    this.change = change;
  }

  @Override
  @Transactional
  public PendingEmailChange request(RequestEmailChangeCommand command) {
    String newEmail = validation.email(command.newEmail());
    MyProfile me =
        profiles
            .findById(command.userId())
            .orElseThrow(
                () ->
                    new UnauthenticatedException(
                        IdentityErrorCodes.REFRESH_TOKEN_INVALID, "Account is not available"));
    // The caller's own current address counts as taken: DD says "khác email hiện tại".
    if (newEmail.equalsIgnoreCase(me.email()) || profiles.emailTaken(newEmail)) {
      throw new ResourceConflictException(
          IdentityErrorCodes.EMAIL_TAKEN, "This e-mail already belongs to an account");
    }
    change.send(command.userId(), newEmail);
    return new PendingEmailChange(newEmail, CODE_TTL_SECONDS);
  }
}
