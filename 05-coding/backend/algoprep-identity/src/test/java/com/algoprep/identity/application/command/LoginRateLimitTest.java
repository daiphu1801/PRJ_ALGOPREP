package com.algoprep.identity.application.command;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.algoprep.common.exception.RateLimitExceededException;
import com.algoprep.common.exception.UnauthenticatedException;
import com.algoprep.identity.application.dto.LoginCommand;
import com.algoprep.identity.application.ports.out.LoginAttemptPort;
import com.algoprep.identity.application.ports.out.PasswordHasherPort;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class LoginRateLimitTest {
  private final LoginAccountService users = mock(LoginAccountService.class);
  private final PasswordHasherPort hasher = mock(PasswordHasherPort.class);
  private final LoginAttemptPort attempts = mock(LoginAttemptPort.class);
  private final SessionIssuer sessions = mock(SessionIssuer.class);
  private final LoginCommandHandler handler =
      new LoginCommandHandler(users, hasher, sessions, attempts);

  @Test
  void blockedPairNeverLooksUpAccountOrChecksPassword() {
    when(attempts.isBlocked("user@example.test", "192.0.2.20")).thenReturn(true);
    assertThatThrownBy(
            () ->
                handler.login(
                    new LoginCommand(" User@Example.Test ", "wrong", false, "192.0.2.20")))
        .isInstanceOf(RateLimitExceededException.class);
    verifyNoInteractions(users, hasher, sessions);
    verify(attempts, never()).recordFailure(anyString(), anyString());
  }

  @Test
  void unknownAccountCountsFailureWithNormalizedEmail() {
    when(users.findByEmail("user@example.test")).thenReturn(Optional.empty());
    assertThatThrownBy(
            () ->
                handler.login(new LoginCommand(" USER@example.test ", "wrong", true, "192.0.2.20")))
        .isInstanceOf(UnauthenticatedException.class);
    verify(hasher).matches("wrong", null);
    verify(attempts).recordFailure("user@example.test", "192.0.2.20");
    verify(attempts, never()).clear(anyString(), anyString());
  }
}
