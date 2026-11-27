package com.algoprep.identity.application.ports.in.avatar;

import com.algoprep.identity.domain.model.auth.OAuthProvider;
import java.util.UUID;

/** BR-28: copy the provider picture into the account's avatar after the first OAuth link. */
public interface ImportOAuthAvatarUseCase {

  /**
   * Queues the import to run after the surrounding transaction commits (at once without one), on a
   * background thread. Never throws: a failure is logged and the login is untouched.
   */
  void scheduleAfterCommit(UUID userId, OAuthProvider provider, String pictureUrl);
}
