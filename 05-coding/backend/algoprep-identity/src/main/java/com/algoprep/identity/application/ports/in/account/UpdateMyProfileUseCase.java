package com.algoprep.identity.application.ports.in.account;

import com.algoprep.identity.application.dto.account.UpdateMyProfileCommand;
import com.algoprep.identity.domain.model.account.MyProfile;

/** Edits the caller's own profile fields and returns the stored result (#10). */
public interface UpdateMyProfileUseCase {
  MyProfile update(UpdateMyProfileCommand command);
}
