package com.algoprep.identity.domain.ports.out;

import com.algoprep.identity.domain.model.RestorableAccount;
import com.algoprep.identity.domain.model.RestorationGrant;
import java.util.Optional;
import java.util.UUID;

/** Row locks serialize restore against status changes and future anonymization jobs. */
public interface AccountRestorationRepository {
  Optional<RestorableAccount> lockById(UUID userId);

  /** Rechecks source, deletion generation, ticket/grace expiry and anonymization in the UPDATE. */
  boolean restore(RestorationGrant grant);
}
