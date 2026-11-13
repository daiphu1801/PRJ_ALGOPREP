package com.algoprep.identity.application.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import com.algoprep.common.exception.UnauthenticatedException;
import com.algoprep.identity.application.dto.RestoreAccountCommand;
import com.algoprep.identity.application.ports.out.RestorationTicketPort;
import com.algoprep.identity.domain.ports.out.AccountRestorationRepository;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class RestoreDeactivatedAccountCommandHandlerTest {
  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {"short", "header.payload.signature", "invalid ticket with spaces"})
  void malformedTicketsHaveGenericErrorWithoutDependencyCalls(String ticket) {
    AccountRestorationRepository accounts = mock(AccountRestorationRepository.class);
    RestorationTicketPort tickets = mock(RestorationTicketPort.class);
    var handler = new RestoreDeactivatedAccountCommandHandler(accounts, tickets);
    assertThatThrownBy(() -> handler.restore(new RestoreAccountCommand(ticket)))
        .isInstanceOfSatisfying(
            UnauthenticatedException.class,
            error -> assertThat(error.errorCode()).isEqualTo("IDT-114"));
    verifyNoInteractions(accounts, tickets);
  }
}
