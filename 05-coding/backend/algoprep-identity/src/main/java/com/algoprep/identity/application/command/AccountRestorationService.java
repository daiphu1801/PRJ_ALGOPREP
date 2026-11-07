package com.algoprep.identity.application.command;

import com.algoprep.identity.application.ports.out.RestorationTicketPort;
import com.algoprep.identity.domain.model.RestorableAccount;
import com.algoprep.identity.domain.model.RestorationGrant;
import com.algoprep.identity.domain.model.RestorationTicket;
import com.algoprep.identity.domain.model.RestorationTicketSecrets;
import com.algoprep.identity.domain.ports.out.AccountRestorationRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Called only after credential verification; OAuth can reuse this after provider verification. */
@Service
class AccountRestorationService {
  private final AccountRestorationRepository accounts;
  private final RestorationTicketPort tickets;

  AccountRestorationService(AccountRestorationRepository accounts, RestorationTicketPort tickets) {
    this.accounts = accounts;
    this.tickets = tickets;
  }

  @Transactional
  public Optional<RestorationTicket> issue(UUID userId) {
    Optional<RestorableAccount> found =
        accounts.lockById(userId).filter(RestorableAccount::canRestore);
    if (found.isEmpty()) {
      return Optional.empty();
    }
    RestorableAccount account = found.orElseThrow();
    String raw = RestorationTicketSecrets.generate();
    RestorationGrant grant =
        new RestorationGrant(
            userId, account.deactivatedAt(), account.databaseTime().plusSeconds(300));
    tickets.save(RestorationTicketSecrets.hash(raw), grant);
    // Login intentionally returns 403; the Redis ticket must survive that expected
    // DB rollback.
    return Optional.of(new RestorationTicket(raw, 300));
  }
}
