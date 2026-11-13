package com.algoprep.identity.application.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.algoprep.common.domain.ports.out.IdGeneratorPort;
import com.algoprep.common.exception.DomainRuleViolationException;
import com.algoprep.common.exception.RateLimitExceededException;
import com.algoprep.common.exception.ResourceConflictException;
import com.algoprep.identity.application.dto.RegisterCommand;
import com.algoprep.identity.application.dto.RegistrationResponse;
import com.algoprep.identity.application.dto.VerifyEmailCommand;
import com.algoprep.identity.application.ports.out.EmailPort;
import com.algoprep.identity.application.ports.out.EmailVerificationOtpPort;
import com.algoprep.identity.application.ports.out.PasswordHasherPort;
import com.algoprep.identity.domain.model.BaseCategory;
import com.algoprep.identity.domain.model.RegistrationAccount;
import com.algoprep.identity.domain.model.UserAccount;
import com.algoprep.identity.domain.model.UserStatus;
import com.algoprep.identity.domain.ports.out.RegistrationRepository;
import jakarta.validation.Validation;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** Register, verify and resend (BR-21) with transaction callbacks fired by hand. */
class EmailVerificationFlowTest {
  private static final String EMAIL = "sv1@test.vn";

  private final RegistrationRepository accounts = mock(RegistrationRepository.class);
  private final EmailVerificationOtpPort otp = mock(EmailVerificationOtpPort.class);
  private final EmailPort mail = mock(EmailPort.class);
  private final PasswordHasherPort hasher = mock(PasswordHasherPort.class);
  private final IdGeneratorPort ids = mock(IdGeneratorPort.class);
  private final EmailVerificationService verification = new EmailVerificationService(otp, mail);
  private final RegistrationValidation validation =
      new RegistrationValidation(Validation.buildDefaultValidatorFactory().getValidator());
  private final RegisterCommandHandler register =
      new RegisterCommandHandler(accounts, hasher, ids, verification, validation);
  private final VerifyEmailCommandHandler verify =
      new VerifyEmailCommandHandler(accounts, verification, validation);
  private final ResendEmailVerificationCommandHandler resend =
      new ResendEmailVerificationCommandHandler(accounts, verification, validation);
  private final UUID userId = UUID.randomUUID();

  @BeforeEach
  void startTransaction() {
    TransactionSynchronizationManager.initSynchronization();
  }

  @AfterEach
  void endTransaction() {
    TransactionSynchronizationManager.clearSynchronization();
  }

  private UserAccount user(UserStatus status, boolean mustChange) {
    return new UserAccount(
        userId, "hash", "Name", status, mustChange, "STUDENT", BaseCategory.STUDENT);
  }

  private static String sha256(String value) throws Exception {
    return HexFormat.of()
        .formatHex(
            MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
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

  private static void assertCode(Throwable error, String code) {
    assertThat(error)
        .isInstanceOfSatisfying(
            DomainRuleViolationException.class, e -> assertThat(e.errorCode()).isEqualTo(code));
  }

  @Test
  void registerCreatesPendingStudentAndMailsASixDigitCode() {
    when(ids.newId()).thenReturn(userId);
    when(hasher.encode("Matkhau123")).thenReturn("hash");
    when(accounts.create(any())).thenReturn(true);
    when(otp.issue(any(), anyString())).thenReturn(true);

    RegistrationResponse response =
        register.register(new RegisterCommand(" Sv1@Test.vn ", "Matkhau123", " Sinh Vien 1 "));

    assertThat(response.email()).isEqualTo(EMAIL);
    ArgumentCaptor<RegistrationAccount> created =
        ArgumentCaptor.forClass(RegistrationAccount.class);
    verify(accounts).create(created.capture());
    assertThat(created.getValue().passwordHash()).isEqualTo("hash");
    assertThat(created.getValue().displayName()).isEqualTo("Sinh Vien 1");
    ArgumentCaptor<String> body = ArgumentCaptor.forClass(String.class);
    verify(mail).send(org.mockito.ArgumentMatchers.eq(EMAIL), anyString(), body.capture());
    assertThat(body.getValue()).containsPattern("[0-9]{6}");
  }

  @Test
  void duplicateEmailIsAConflictAndSendsNothing() {
    when(ids.newId()).thenReturn(userId);
    when(hasher.encode(anyString())).thenReturn("hash");
    when(accounts.create(any())).thenReturn(false);
    assertThatThrownBy(
            () -> register.register(new RegisterCommand(EMAIL, "Matkhau123", "Sinh Vien 1")))
        .isInstanceOfSatisfying(
            ResourceConflictException.class, e -> assertThat(e.errorCode()).isEqualTo("IDT-101"));
    verify(mail, never()).send(anyString(), anyString(), anyString());
  }

  @Test
  void cooldownBlocksANewCodeBeforeAnyMailIsSent() {
    when(ids.newId()).thenReturn(userId);
    when(hasher.encode(anyString())).thenReturn("hash");
    when(accounts.create(any())).thenReturn(true);
    when(otp.issue(any(), anyString())).thenReturn(false);
    assertThatThrownBy(
            () -> register.register(new RegisterCommand(EMAIL, "Matkhau123", "Sinh Vien 1")))
        .isInstanceOfSatisfying(
            RateLimitExceededException.class, e -> assertThat(e.errorCode()).isEqualTo("IDT-V12"));
    verify(mail, never()).send(anyString(), anyString(), anyString());
  }

  @Test
  void rolledBackRegistrationClearsTheIssuedCodeButCommittedOneKeepsIt() {
    when(ids.newId()).thenReturn(userId);
    when(hasher.encode(anyString())).thenReturn("hash");
    when(accounts.create(any())).thenReturn(true);
    when(otp.issue(any(), anyString())).thenReturn(true);
    register.register(new RegisterCommand(EMAIL, "Matkhau123", "Sinh Vien 1"));
    commit();
    verify(otp, never()).clear(any(), anyString());

    TransactionSynchronizationManager.clearSynchronization();
    TransactionSynchronizationManager.initSynchronization();
    register.register(new RegisterCommand(EMAIL, "Matkhau123", "Sinh Vien 1"));
    rollback();
    verify(otp).clear(org.mockito.ArgumentMatchers.eq(userId), anyString());
  }

  @Test
  void cleanupFailureAfterRollbackIsSwallowed() {
    when(ids.newId()).thenReturn(userId);
    when(hasher.encode(anyString())).thenReturn("hash");
    when(accounts.create(any())).thenReturn(true);
    when(otp.issue(any(), anyString())).thenReturn(true);
    doThrow(new IllegalStateException("redis down")).when(otp).clear(any(), anyString());
    register.register(new RegisterCommand(EMAIL, "Matkhau123", "Sinh Vien 1"));
    assertThatCode(this::rollback).doesNotThrowAnyException();
  }

  @Test
  void verifyRejectsMalformedCodesAndUnknownAccountsWithTheSameError() {
    for (String bad : new String[] {null, "", "12345", "1234567", "abcdef", "12 456"}) {
      assertThatThrownBy(() -> verify.verify(new VerifyEmailCommand(EMAIL, bad)))
          .satisfies(e -> assertCode(e, "IDT-112"));
    }
    when(accounts.lockByEmail(EMAIL)).thenReturn(Optional.empty());
    assertThatThrownBy(() -> verify.verify(new VerifyEmailCommand(EMAIL, "123456")))
        .satisfies(e -> assertCode(e, "IDT-112"));
  }

  @Test
  void verifyActivatesAndClearsTheCodeOnlyAfterCommit() throws Exception {
    when(accounts.lockByEmail(EMAIL))
        .thenReturn(Optional.of(user(UserStatus.PENDING_VERIFICATION, false)));
    when(otp.matches(userId, sha256("123456"))).thenReturn(true);

    verify.verify(new VerifyEmailCommand(EMAIL, "123456"));

    verify(accounts).activate(userId);
    verify(otp, never()).clear(any(), anyString());
    commit();
    verify(otp).clear(userId, sha256("123456"));
  }

  @Test
  void verifyRejectsWrongCodeAdminCreatedAndDeactivatedAccounts() {
    when(accounts.lockByEmail(EMAIL))
        .thenReturn(Optional.of(user(UserStatus.PENDING_VERIFICATION, false)));
    when(otp.matches(any(), anyString())).thenReturn(false);
    assertThatThrownBy(() -> verify.verify(new VerifyEmailCommand(EMAIL, "123456")))
        .satisfies(e -> assertCode(e, "IDT-112"));

    when(accounts.lockByEmail(EMAIL))
        .thenReturn(Optional.of(user(UserStatus.PENDING_VERIFICATION, true)));
    assertThatThrownBy(() -> verify.verify(new VerifyEmailCommand(EMAIL, "123456")))
        .satisfies(e -> assertCode(e, "IDT-112"));

    when(accounts.lockByEmail(EMAIL)).thenReturn(Optional.of(user(UserStatus.DEACTIVATED, false)));
    assertThatThrownBy(() -> verify.verify(new VerifyEmailCommand(EMAIL, "123456")))
        .satisfies(e -> assertCode(e, "IDT-112"));
    verify(accounts, never()).activate(any());
  }

  @Test
  void verifyOnAnAlreadyActiveAccountIsANoOp() {
    when(accounts.lockByEmail(EMAIL)).thenReturn(Optional.of(user(UserStatus.ACTIVE, false)));
    verify.verify(new VerifyEmailCommand(EMAIL, "123456"));
    verify(accounts, never()).activate(any());
    verify(otp, never()).matches(any(), anyString());
  }

  @Test
  void cleanupFailureAfterCommitDoesNotUndoTheVerification() throws Exception {
    when(accounts.lockByEmail(EMAIL))
        .thenReturn(Optional.of(user(UserStatus.PENDING_VERIFICATION, false)));
    when(otp.matches(any(), anyString())).thenReturn(true);
    doThrow(new IllegalStateException("redis down")).when(otp).clear(any(), anyString());
    verify.verify(new VerifyEmailCommand(EMAIL, "123456"));
    assertThatCode(this::commit).doesNotThrowAnyException();
  }

  @Test
  void resendOnlyMailsSelfRegisteredPendingAccounts() {
    when(otp.issue(any(), anyString())).thenReturn(true);

    when(accounts.lockByEmail(EMAIL)).thenReturn(Optional.empty());
    resend.resend(EMAIL);
    when(accounts.lockByEmail(EMAIL)).thenReturn(Optional.of(user(UserStatus.ACTIVE, false)));
    resend.resend(EMAIL);
    when(accounts.lockByEmail(EMAIL))
        .thenReturn(Optional.of(user(UserStatus.PENDING_VERIFICATION, true)));
    resend.resend(EMAIL);
    verify(mail, never()).send(anyString(), anyString(), anyString());

    when(accounts.lockByEmail(EMAIL))
        .thenReturn(Optional.of(user(UserStatus.PENDING_VERIFICATION, false)));
    resend.resend(" SV1@test.vn ");
    verify(mail).send(org.mockito.ArgumentMatchers.eq(EMAIL), anyString(), anyString());
  }
}
