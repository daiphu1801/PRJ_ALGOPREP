package com.algoprep.identity.application.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.algoprep.common.exception.AlgoPrepException;
import com.algoprep.common.exception.UnauthenticatedException;
import com.algoprep.identity.application.dto.ChangeMyPasswordCommand;
import com.algoprep.identity.application.ports.out.LoginAttemptPort;
import com.algoprep.identity.application.ports.out.PasswordHasherPort;
import com.algoprep.identity.domain.model.AuditEntry;
import com.algoprep.identity.domain.model.BaseCategory;
import com.algoprep.identity.domain.model.PasswordChangeAccount;
import com.algoprep.identity.domain.model.UserAccount;
import com.algoprep.identity.domain.model.UserStatus;
import com.algoprep.identity.domain.ports.out.AuditLogPort;
import com.algoprep.identity.domain.ports.out.PasswordChangeRepository;
import jakarta.validation.Validation;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/** BR-06 order of checks: new password rules, then current password, then write and audit. */
class ChangeMyPasswordCommandHandlerTest {
  private final PasswordChangeRepository accounts = mock(PasswordChangeRepository.class);
  private final PasswordHasherPort passwords = mock(PasswordHasherPort.class);
  private final AuditLogPort audit = mock(AuditLogPort.class);
  private final LoginAttemptPort attempts = mock(LoginAttemptPort.class);
  private final ChangeMyPasswordCommandHandler handler =
      new ChangeMyPasswordCommandHandler(
          accounts,
          passwords,
          new RegistrationValidation(Validation.buildDefaultValidatorFactory().getValidator()),
          audit,
          attempts);

  private final UUID userId = UUID.randomUUID();
  private final UUID sessionId = UUID.randomUUID();

  private void accountWithHash(String hash) {
    UserAccount user =
        new UserAccount(
            userId, hash, "Display Name", UserStatus.ACTIVE, true, "STUDENT", BaseCategory.STUDENT);
    when(accounts.lockById(userId))
        .thenReturn(Optional.of(new PasswordChangeAccount(user, "a@example.test")));
  }

  private ChangeMyPasswordCommand command(String current, String next) {
    return new ChangeMyPasswordCommand(userId, sessionId, current, next);
  }

  private static void assertCode(Throwable error, String code) {
    assertThat(error)
        .isInstanceOfSatisfying(
            AlgoPrepException.class, e -> assertThat(e.errorCode()).isEqualTo(code));
  }

  @Test
  void correctCurrentPasswordStoresHashKeepsCallerSessionAndAudits() {
    accountWithHash("old-hash");
    when(passwords.matches("OldPass123", "old-hash")).thenReturn(true);
    when(passwords.encode("NewPass456")).thenReturn("new-hash");

    handler.change(command("OldPass123", "NewPass456"));

    verify(accounts).updatePasswordAndRevokeOtherSessions(userId, "new-hash", sessionId);
    ArgumentCaptor<AuditEntry> entry = ArgumentCaptor.forClass(AuditEntry.class);
    verify(audit).record(entry.capture());
    assertThat(entry.getValue().actionType()).isEqualTo("PASSWORD_CHANGED");
    assertThat(entry.getValue().beforeJson()).isNull();
    assertThat(entry.getValue().afterJson()).isNull();
  }

  @Test
  void wrongCurrentPasswordIs422Idt115WithoutWrites() {
    accountWithHash("old-hash");
    when(passwords.matches("Wrong123x", "old-hash")).thenReturn(false);

    assertThatThrownBy(() -> handler.change(command("Wrong123x", "NewPass456")))
        .satisfies(e -> assertCode(e, "IDT-115"));

    verify(accounts, never()).updatePasswordAndRevokeOtherSessions(any(), any(), any());
    verify(audit, never()).record(any());
  }

  @Test
  void weakNewPasswordIsRejectedBeforeCurrentPasswordIsChecked() {
    accountWithHash("old-hash");

    assertThatThrownBy(() -> handler.change(command("Wrong123x", "short1")))
        .satisfies(e -> assertCode(e, "IDT-V01"));

    verify(passwords, never()).matches(any(), any());
  }

  @Test
  void accountWithoutPasswordIsTreatedAsWrongCurrentPassword() {
    accountWithHash(null);
    when(passwords.matches("Whatever123", null)).thenReturn(false);

    assertThatThrownBy(() -> handler.change(command("Whatever123", "NewPass456")))
        .satisfies(e -> assertCode(e, "IDT-115"));
  }

  @Test
  void missingAccountIs401() {
    when(accounts.lockById(userId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> handler.change(command("OldPass123", "NewPass456")))
        .isInstanceOf(UnauthenticatedException.class);
  }

  @Test
  void aBlockedAccountIsRefusedBeforeTheCurrentPasswordIsEvenChecked() {
    accountWithHash("stored");
    when(attempts.isBlocked("pwchange:" + userId, "-")).thenReturn(true);

    assertThatThrownBy(() -> handler.change(command("Whatever1!", "NewPassword123")))
        .satisfies(e -> assertCode(e, "IDT-103"));
    verify(passwords, never()).matches(any(), any());
  }

  @Test
  void aWrongCurrentPasswordCountsAgainstTheAccount() {
    accountWithHash("stored");
    when(passwords.matches("wrong", "stored")).thenReturn(false);

    assertThatThrownBy(() -> handler.change(command("wrong", "NewPassword123")))
        .satisfies(e -> assertCode(e, "IDT-115"));
    verify(attempts).recordFailure("pwchange:" + userId, "-");
  }
}
