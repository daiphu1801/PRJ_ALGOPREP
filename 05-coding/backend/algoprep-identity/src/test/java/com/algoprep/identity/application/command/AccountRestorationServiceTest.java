package com.algoprep.identity.application.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.algoprep.identity.application.dto.RestoreAccountCommand;
import com.algoprep.identity.application.ports.out.RestorationTicketPort;
import com.algoprep.identity.domain.exception.AccountDeactivatedException;
import com.algoprep.identity.domain.model.RestorableAccount;
import com.algoprep.identity.domain.model.RestorationGrant;
import com.algoprep.identity.domain.model.RestorationTicket;
import com.algoprep.identity.domain.model.RestorationTicketSecrets;
import com.algoprep.identity.domain.model.UserStatus;
import com.algoprep.identity.domain.ports.out.AccountRestorationRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class AccountRestorationServiceTest {
  private static final Instant NOW = Instant.parse("2026-10-30T10:00:00Z");
  private final AccountRestorationRepository accounts = mock(AccountRestorationRepository.class);
  private final RestorationTicketPort tickets = mock(RestorationTicketPort.class);
  private final AccountRestorationService service =
      new AccountRestorationService(accounts, tickets);

  @Test
  void issuanceBindsAccountAndDeletionAndStoresOnlyTheHash() {
    UUID id = UUID.randomUUID();
    Instant deletion = NOW.minusSeconds(60);
    when(accounts.lockById(id))
        .thenReturn(
            Optional.of(
                new RestorableAccount(id, UserStatus.DEACTIVATED, "SELF", deletion, null, NOW)));
    RestorationTicket ticket = service.issue(id).orElseThrow();
    assertThat(ticket.value()).matches("[A-Za-z0-9_-]{43}");
    assertThat(ticket.expiresInSeconds()).isEqualTo(300);
    ArgumentCaptor<RestorationGrant> grant = ArgumentCaptor.forClass(RestorationGrant.class);
    verify(tickets)
        .save(
            org.mockito.ArgumentMatchers.eq(RestorationTicketSecrets.hash(ticket.value())),
            grant.capture());
    assertThat(grant.getValue())
        .isEqualTo(new RestorationGrant(id, deletion, NOW.plusSeconds(300)));
  }

  @Test
  void absentAndAdminLockedAccountCannotIssueTicket() {
    UUID absent = UUID.randomUUID();
    UUID admin = UUID.randomUUID();
    when(accounts.lockById(absent)).thenReturn(Optional.empty());
    when(accounts.lockById(admin))
        .thenReturn(
            Optional.of(
                new RestorableAccount(
                    admin, UserStatus.DEACTIVATED, "ADMIN", NOW.minusSeconds(60), null, NOW)));
    assertThat(service.issue(absent)).isEmpty();
    assertThat(service.issue(admin)).isEmpty();
    verifyNoInteractions(tickets);
  }

  @Test
  void ticketExceptionAndCommandDoNotPrintSecrets() {
    String secret = RestorationTicketSecrets.generate();
    RestorationTicket ticket = new RestorationTicket(secret, 300);
    AccountDeactivatedException exception = new AccountDeactivatedException(ticket);
    assertThat(ticket.toString()).doesNotContain(secret);
    assertThat(exception.toString()).doesNotContain(secret);
    assertThat(exception.getMessage()).isEqualTo("Account is deactivated");
    assertThat(exception.clientErrorDetails())
        .containsEntry("restoreTicket", secret)
        .containsEntry("expiresInSeconds", 300);
    assertThat(new RestoreAccountCommand(secret).toString()).doesNotContain(secret);
    assertThat(new AccountDeactivatedException(null).clientErrorDetails()).isEmpty();
  }
}
