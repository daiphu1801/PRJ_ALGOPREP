package com.algoprep.identity.application.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.algoprep.common.exception.DomainRuleViolationException;
import com.algoprep.identity.application.dto.ResetPasswordCommand;
import com.algoprep.identity.application.dto.VerifyPasswordResetOtpCommand;
import com.algoprep.identity.application.ports.out.EmailPort;
import com.algoprep.identity.application.ports.out.PasswordHasherPort;
import com.algoprep.identity.application.ports.out.PasswordResetOtpPort;
import com.algoprep.identity.domain.model.AuditEntry;
import com.algoprep.identity.domain.model.BaseCategory;
import com.algoprep.identity.domain.model.PasswordResetAccount;
import com.algoprep.identity.domain.model.PasswordResetChallenge;
import com.algoprep.identity.domain.model.UserAccount;
import com.algoprep.identity.domain.model.UserStatus;
import com.algoprep.identity.domain.ports.out.AuditLogPort;
import com.algoprep.identity.domain.ports.out.PasswordResetRepository;
import jakarta.validation.Validation;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** Forgot, verify-otp and reset (BR-04): send paths, guidance mail and transaction callbacks. */
class PasswordResetHandlersTest {
  private static final String EMAIL = "sv1@test.vn";
  private static final Instant ISSUED = Instant.parse("2026-10-31T01:00:00Z");

  private final PasswordResetOtpPort otp = mock(PasswordResetOtpPort.class);
  private final EmailPort mail = mock(EmailPort.class);
  private final AuditLogPort audit = mock(AuditLogPort.class);
  private final PasswordResetRepository accounts = mock(PasswordResetRepository.class);
  private final PasswordHasherPort hasher = mock(PasswordHasherPort.class);
  private final PasswordResetService service = new PasswordResetService(otp, mail, audit);
  private final RegistrationValidation validation =
      new RegistrationValidation(Validation.buildDefaultValidatorFactory().getValidator());
  private final ForgotPasswordCommandHandler forgot =
      new ForgotPasswordCommandHandler(accounts, service, validation);
  private final VerifyPasswordResetOtpCommandHandler verify =
      new VerifyPasswordResetOtpCommandHandler(accounts, service, validation);
  private final ResetPasswordCommandHandler reset =
      new ResetPasswordCommandHandler(accounts, service, hasher, validation, audit);
  private final UUID userId = UUID.randomUUID();

  @BeforeEach
  void startTransaction() {
    TransactionSynchronizationManager.initSynchronization();
  }

  @AfterEach
  void endTransaction() {
    TransactionSynchronizationManager.clearSynchronization();
  }

  private PasswordResetAccount account(
      String hash, UserStatus status, boolean mustChange, Instant changedAt) {
    UserAccount user =
        new UserAccount(
            userId, hash, "Sinh Vien", status, mustChange, "STUDENT", BaseCategory.STUDENT);
    return new PasswordResetAccount(user, EMAIL, changedAt, ISSUED);
  }

  private PasswordResetAccount active() {
    return account("hash", UserStatus.ACTIVE, false, null);
  }

  private void commit() {
    List<TransactionSynchronization> callbacks =
        List.copyOf(TransactionSynchronizationManager.getSynchronizations());
    callbacks.forEach(TransactionSynchronization::afterCommit);
    callbacks.forEach(c -> c.afterCompletion(TransactionSynchronization.STATUS_COMMITTED));
  }

  private void rollback() {
    List.copyOf(TransactionSynchronizationManager.getSynchronizations())
        .forEach(c -> c.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));
  }

  @Test
  void forgotIssuesACodeAuditsAndMailsIt() {
    when(accounts.lockByEmail(EMAIL)).thenReturn(Optional.of(active()));
    when(otp.issue(eq(userId), any())).thenReturn(true);

    forgot.forgot(" SV1@test.vn ");

    ArgumentCaptor<PasswordResetChallenge> challenge =
        ArgumentCaptor.forClass(PasswordResetChallenge.class);
    verify(otp).issue(eq(userId), challenge.capture());
    assertThat(challenge.getValue().digest()).hasSize(64);
    assertThat(challenge.getValue().issuedAt()).isEqualTo(ISSUED);
    ArgumentCaptor<AuditEntry> entry = ArgumentCaptor.forClass(AuditEntry.class);
    verify(audit).record(entry.capture());
    assertThat(entry.getValue().actionType()).isEqualTo("PASSWORD_RESET_REQUESTED");
    ArgumentCaptor<String> body = ArgumentCaptor.forClass(String.class);
    verify(mail).send(eq(EMAIL), anyString(), body.capture());
    assertThat(body.getValue()).containsPattern("[0-9]{6}");
  }

  @Test
  void forgotForUnknownCooldownOrIneligibleAccountsDoesNothingVisible() {
    when(accounts.lockByEmail(EMAIL)).thenReturn(Optional.empty());
    forgot.forgot(EMAIL);

    when(accounts.lockByEmail(EMAIL)).thenReturn(Optional.of(active()));
    when(otp.issue(any(), any())).thenReturn(false);
    forgot.forgot(EMAIL);

    when(accounts.lockByEmail(EMAIL))
        .thenReturn(Optional.of(account("hash", UserStatus.PENDING_VERIFICATION, false, null)));
    forgot.forgot(EMAIL);

    verifyNoInteractions(audit, mail);
  }

  @Test
  void oauthOnlyAccountGetsGuidanceInsteadOfACode() {
    when(accounts.lockByEmail(EMAIL))
        .thenReturn(Optional.of(account(null, UserStatus.ACTIVE, false, null)));
    when(otp.reserveGuidance(userId, ISSUED)).thenReturn(true);
    forgot.forgot(EMAIL);
    verify(mail).send(eq(EMAIL), anyString(), anyString());
    verify(otp, never()).issue(any(), any());
    rollback();
    verify(otp).clear(userId, ISSUED);

    when(otp.reserveGuidance(userId, ISSUED)).thenReturn(false);
    forgot.forgot(EMAIL);
    verify(mail).send(anyString(), anyString(), anyString());
  }

  @Test
  void rollbackClearsTheIssuedCodeAndCleanupFailureIsSwallowed() {
    when(accounts.lockByEmail(EMAIL)).thenReturn(Optional.of(active()));
    when(otp.issue(any(), any())).thenReturn(true);
    doThrow(new IllegalStateException("redis down")).when(otp).clear(any(), any());
    forgot.forgot(EMAIL);
    assertThatCode(this::rollback).doesNotThrowAnyException();
    verify(otp).clear(userId, ISSUED);
  }

  @Test
  void committedForgotKeepsTheCode() {
    when(accounts.lockByEmail(EMAIL)).thenReturn(Optional.of(active()));
    when(otp.issue(any(), any())).thenReturn(true);
    forgot.forgot(EMAIL);
    commit();
    verify(otp, never()).clear(any(), any());
  }

  @Test
  void verifyChecksTheCodeWithoutChangingAnything() {
    when(accounts.lockByEmail(EMAIL)).thenReturn(Optional.of(active()));
    when(otp.verify(eq(userId), anyString())).thenReturn(Optional.of(ISSUED));
    verify.verify(new VerifyPasswordResetOtpCommand(EMAIL, "123456"));
    verify(accounts, never()).updatePasswordAndRevokeSessions(any(), anyString());
    verify(otp, never()).clear(any(), any());
  }

  @Test
  void verifyForAnUnknownEmailFailsGenerically() {
    when(accounts.lockByEmail(EMAIL)).thenReturn(Optional.empty());
    when(otp.verify(any(), anyString())).thenReturn(Optional.empty());
    assertThatThrownBy(() -> verify.verify(new VerifyPasswordResetOtpCommand(EMAIL, "123456")))
        .isInstanceOfSatisfying(
            DomainRuleViolationException.class,
            e -> assertThat(e.errorCode()).isEqualTo("IDT-107"));
  }

  @Test
  void resetChangesThePasswordAuditsAndClearsTheCodeAfterCommit() {
    when(accounts.lockByEmail(EMAIL)).thenReturn(Optional.of(active()));
    when(otp.verify(eq(userId), anyString())).thenReturn(Optional.of(ISSUED));
    when(hasher.encode("Matkhaumoi456")).thenReturn("new-hash");

    reset.reset(new ResetPasswordCommand(EMAIL, "123456", "Matkhaumoi456"));

    verify(accounts).updatePasswordAndRevokeSessions(userId, "new-hash");
    ArgumentCaptor<AuditEntry> entry = ArgumentCaptor.forClass(AuditEntry.class);
    verify(audit).record(entry.capture());
    assertThat(entry.getValue().actionType()).isEqualTo("PASSWORD_RESET_COMPLETED");
    verify(otp, never()).clear(any(), any());
    commit();
    verify(otp).clear(userId, ISSUED);
  }

  @Test
  void resetWithAWeakPasswordOrBadCodeChangesNothing() {
    when(accounts.lockByEmail(EMAIL)).thenReturn(Optional.of(active()));
    when(otp.verify(eq(userId), anyString())).thenReturn(Optional.of(ISSUED));
    assertThatThrownBy(() -> reset.reset(new ResetPasswordCommand(EMAIL, "123456", "short")))
        .isInstanceOf(DomainRuleViolationException.class);

    when(otp.verify(eq(userId), anyString())).thenReturn(Optional.empty());
    assertThatThrownBy(
            () -> reset.reset(new ResetPasswordCommand(EMAIL, "123456", "Matkhaumoi456")))
        .isInstanceOf(DomainRuleViolationException.class);

    verify(accounts, never()).updatePasswordAndRevokeSessions(any(), anyString());
    verifyNoInteractions(audit);
  }

  @Test
  void resetForAnUnknownEmailFailsGenerically() {
    when(accounts.lockByEmail(EMAIL)).thenReturn(Optional.empty());
    when(otp.verify(any(), anyString())).thenReturn(Optional.empty());
    assertThatThrownBy(
            () -> reset.reset(new ResetPasswordCommand(EMAIL, "123456", "Matkhaumoi456")))
        .isInstanceOfSatisfying(
            DomainRuleViolationException.class,
            e -> assertThat(e.errorCode()).isEqualTo("IDT-107"));
  }
}
