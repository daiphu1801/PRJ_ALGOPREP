package com.algoprep.identity.application.ports.in.avatar;

import com.algoprep.identity.application.dto.avatar.AvatarContent;
import java.util.UUID;

/** Reads any user's avatar for any signed-in caller (#76, BR-27). */
public interface GetUserAvatarUseCase {

  /**
   * Fails with {@code IDT-451} for every reason there is nothing to show. {@code ifNoneMatch} is
   * the raw request header, or null.
   */
  AvatarContent get(UUID userId, String ifNoneMatch);
}
