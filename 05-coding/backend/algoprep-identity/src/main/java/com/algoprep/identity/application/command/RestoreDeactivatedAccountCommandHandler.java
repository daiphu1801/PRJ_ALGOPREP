package com.algoprep.identity.application.command;

import com.algoprep.common.exception.UnauthenticatedException;
import com.algoprep.identity.application.dto.RestoreAccountCommand;
import com.algoprep.identity.application.ports.in.RestoreDeactivatedAccountUseCase;
import com.algoprep.identity.application.ports.out.RestorationTicketPort;
import com.algoprep.identity.domain.model.RestorableAccount;
import com.algoprep.identity.domain.model.RestorationGrant;
import com.algoprep.identity.domain.model.RestorationTicketSecrets;
import com.algoprep.identity.domain.ports.out.AccountRestorationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** The locked DB state enforces one-time restore; Redis cleanup follows commit. */
@Service
class RestoreDeactivatedAccountCommandHandler implements RestoreDeactivatedAccountUseCase {
  private static final Logger LOG =
      LoggerFactory.getLogger(RestoreDeactivatedAccountCommandHandler.class);
  private final AccountRestorationRepository accounts;
  private final RestorationTicketPort tickets;

  RestoreDeactivatedAccountCommandHandler(
      AccountRestorationRepository accounts, RestorationTicketPort tickets) {
    this.accounts = accounts;
    this.tickets = tickets;
  }

  @Override
  @Transactional
  public void restore(RestoreAccountCommand command) {
    String raw = command.restoreTicket();
    if (raw == null || !raw.matches("[A-Za-z0-9_-]{43}")) {
      throw invalid();
    }
    String hash = RestorationTicketSecrets.hash(raw);
    RestorationGrant grant =
        tickets.find(hash).orElseThrow(RestoreDeactivatedAccountCommandHandler::invalid);
    RestorableAccount account =
        accounts
            .lockById(grant.userId())
            .orElseThrow(RestoreDeactivatedAccountCommandHandler::invalid);
    if (!account.canRestore()
        || !grant.deactivatedAt().equals(account.deactivatedAt())
        || !grant.expiresAt().isAfter(account.databaseTime())
        || !accounts.restore(grant)) {
      throw invalid();
    }
    TransactionSynchronizationManager.registerSynchronization(
        new TransactionSynchronization() {
          @Override
          public void afterCommit() {
            try {
              tickets.delete(hash);
            } catch (RuntimeException ex) {
              LOG.warn("Restoration ticket cleanup unavailable; original expiry is retained");
            }
          }
        });
  }

  private static UnauthenticatedException invalid() {
    return new UnauthenticatedException("IDT-114", "Restoration ticket is invalid or unavailable");
  }
}
