package com.algoprep.identity.application.command.auth;

import com.algoprep.identity.application.dto.UserSummary;
import com.algoprep.identity.application.dto.auth.LoginResult;
import com.algoprep.identity.application.ports.in.avatar.ImportOAuthAvatarUseCase;
import com.algoprep.identity.domain.model.UserAccount;
import com.algoprep.identity.domain.model.auth.OAuthProfile;
import org.springframework.stereotype.Service;

/**
 * Last part of the OAuth login (BR-03 step 6): admit by status, open the session, then queue the
 * BR-28 avatar import for a first link. The import is queued after the session exists and never
 * influences the answer.
 */
@Service
class OAuthLoginFinisher {
  private final LoginAccountService admission;
  private final SessionIssuer sessions;
  private final ImportOAuthAvatarUseCase avatars;

  OAuthLoginFinisher(
      LoginAccountService admission, SessionIssuer sessions, ImportOAuthAvatarUseCase avatars) {
    this.admission = admission;
    this.sessions = sessions;
    this.avatars = avatars;
  }

  LoginResult finish(UserAccount user, OAuthProfile profile, boolean firstLink) {
    admission.admitByStatus(user);
    SessionIssuer.Session session = sessions.openNewFamily(user);
    admission.touchLastActive(user.id());
    if (firstLink) {
      avatars.scheduleAfterCommit(user.id(), profile.provider(), profile.pictureUrl());
    }
    UserSummary summary =
        new UserSummary(
            user.id(), user.displayName(), user.baseCategory(), user.mustChangePassword());
    return new LoginResult(
        session.accessToken().value(),
        session.expiresInSeconds(),
        summary,
        session.rawRefreshToken());
  }
}
