package com.algoprep.identity.application.ports.in.account;

import com.algoprep.identity.domain.model.account.MyProfile;
import java.util.UUID;

/** Loads the caller's own profile (#9). */
public interface GetMyProfileUseCase {
  MyProfile get(UUID userId);
}
