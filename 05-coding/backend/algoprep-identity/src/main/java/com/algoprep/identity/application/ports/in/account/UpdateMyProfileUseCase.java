package com.algoprep.identity.application.ports.in;

import com.algoprep.identity.application.dto.UpdateMyProfileCommand;
import com.algoprep.identity.domain.model.MyProfile;

/** Edits the caller's own profile fields and returns the stored result (#10). */
public interface UpdateMyProfileUseCase {
  MyProfile update(UpdateMyProfileCommand command);
}
