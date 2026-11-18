package com.algoprep.identity.application.query.avatar;

import com.algoprep.common.exception.ResourceNotFoundException;
import com.algoprep.identity.application.dto.avatar.AvatarContent;
import com.algoprep.identity.application.ports.in.avatar.GetUserAvatarUseCase;
import com.algoprep.identity.application.ports.out.AvatarObjectStorePort;
import com.algoprep.identity.domain.exception.IdentityErrorCodes;
import com.algoprep.identity.domain.model.avatar.AvatarRecord;
import com.algoprep.identity.domain.ports.out.UserAvatarRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * BR-27: one 404 for no row, unknown, deactivated and anonymised accounts. The transaction covers
 * only the metadata read; the bytes come from the returned supplier, which the controller calls
 * after the transaction ended, so no connection is held while the object store answers.
 */
@Service
class GetUserAvatarQueryHandler implements GetUserAvatarUseCase {
  private final UserAvatarRepository avatars;
  private final AvatarObjectStorePort store;

  GetUserAvatarQueryHandler(UserAvatarRepository avatars, AvatarObjectStorePort store) {
    this.avatars = avatars;
    this.store = store;
  }

  @Override
  @Transactional(readOnly = true)
  public AvatarContent get(UUID userId, String ifNoneMatch) {
    AvatarRecord row =
        avatars
            .findReadable(userId)
            .orElseThrow(
                () ->
                    new ResourceNotFoundException(
                        IdentityErrorCodes.AVATAR_NOT_FOUND, "No avatar to show"));
    if (matches(ifNoneMatch, row.etag())) {
      return new AvatarContent(row.contentType(), row.etag(), true, null);
    }
    String key = row.objectKey();
    return new AvatarContent(row.contentType(), row.etag(), false, () -> store.get(key));
  }

  /** Accepts {@code *}, a comma list and the weak {@code W/} prefix (RFC 9110 section 13.1.2). */
  private static boolean matches(String header, String etag) {
    if (header == null) {
      return false;
    }
    for (String candidate : header.split(",")) {
      String value = candidate.trim();
      if (value.startsWith("W/")) {
        value = value.substring(2);
      }
      if (value.equals("*") || value.equals(etag)) {
        return true;
      }
    }
    return false;
  }
}
