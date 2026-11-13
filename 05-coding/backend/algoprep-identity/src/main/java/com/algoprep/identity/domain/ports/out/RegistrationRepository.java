package com.algoprep.identity.domain.ports.out;

import com.algoprep.identity.domain.model.RegistrationAccount;
import com.algoprep.identity.domain.model.UserAccount;
import java.util.Optional;
import java.util.UUID;

/** Persistence operations specific to self-registration and email verification. */
public interface RegistrationRepository {
  /** Creates a pending STUDENT with mustChangePassword=false; false means duplicate email. */
  boolean create(RegistrationAccount account);

  /** Locks the user until the current transaction ends, serializing verify and resend. */
  Optional<UserAccount> lockByEmail(String email);

  /** Activates only a pending self-registered account. */
  void activate(UUID userId);
}
