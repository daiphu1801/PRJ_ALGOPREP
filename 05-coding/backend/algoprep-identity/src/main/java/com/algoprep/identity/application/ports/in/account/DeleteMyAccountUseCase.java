package com.algoprep.identity.application.ports.in.account;

import java.util.UUID;

/** Starts the caller's self-deletion after they retype their e-mail (#18, BR-08). */
public interface DeleteMyAccountUseCase {
  void delete(UUID userId, String emailConfirmation);
}
