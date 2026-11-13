package com.algoprep.identity.application.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.algoprep.common.exception.DomainRuleViolationException;
import com.algoprep.identity.application.dto.ResetPasswordCommand;
import com.algoprep.identity.application.dto.VerifyPasswordResetOtpCommand;
import com.algoprep.identity.application.ports.out.EmailPort;
import com.algoprep.identity.application.ports.out.PasswordResetOtpPort;
import com.algoprep.identity.domain.model.BaseCategory;
import com.algoprep.identity.domain.model.PasswordResetAccount;
import com.algoprep.identity.domain.model.PasswordResetChallenge;
import com.algoprep.identity.domain.model.UserAccount;
import com.algoprep.identity.domain.model.UserStatus;
import com.algoprep.identity.domain.ports.out.AuditLogPort;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;

/** Non-consuming verification and the PostgreSQL timestamp replay boundary (BR-04). */
class PasswordResetServiceTest {
  private static final Instant ISSUED = Instant.parse("2026-10-30T10:00:00Z");
  private final PasswordResetOtpPort otp = mock(PasswordResetOtpPort.class);
  private final EmailPort mail = mock(EmailPort.class);
  private final AuditLogPort audit = mock(AuditLogPort.class);
  private final PasswordResetService service = new PasswordResetService(otp, mail, audit);

  @Test
  void verificationDoesNotConsumeCodeOrIssueSessionOrMail() {
    PasswordResetAccount account = account(ISSUED.minusSeconds(1), "hash");
    when(otp.verify(any(), anyString())).thenReturn(Optional.of(ISSUED));
    assertThat(service.check(account, "123456")).isEqualTo(ISSUED);
    verify(otp, never()).clear(any(), any());
    verifyNoInteractions(mail, audit);
  }

  @ParameterizedTest
  @ValueSource(longs = {0, 1})
  void committedPasswordChangeInvalidatesRetainedOtpAtOrAfterIssuance(long seconds) {
    when(otp.verify(any(), anyString())).thenReturn(Optional.of(ISSUED));
    assertInvalid(() -> service.check(account(ISSUED.plusSeconds(seconds), "hash"), "123456"));
    verify(otp, never()).clear(any(), any());
    verifyNoInteractions(mail, audit);
  }

  @Test
  void unknownAndOauthAccountsStillCheckRedisButCannotUseFabricatedChallenge() {
    when(otp.verify(any(), anyString())).thenReturn(Optional.of(ISSUED));
    assertInvalid(() -> service.check(null, "123456"));
    assertInvalid(() -> service.check(account(null, null), "123456"));
    ArgumentCaptor<UUID> userIds = ArgumentCaptor.forClass(UUID.class);
    org.mockito.Mockito.verify(otp, org.mockito.Mockito.times(2))
        .verify(userIds.capture(), anyString());
    assertThat(userIds.getAllValues()).containsOnly(new UUID(0, 0));
    verifyNoInteractions(mail, audit);
  }

  @Test
  void missingRedisCodeIsGenericFailure() {
    when(otp.verify(any(), anyString())).thenReturn(Optional.empty());
    assertInvalid(() -> service.check(account(null, "hash"), "123456"));
    verifyNoInteractions(mail, audit);
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {"12345", "abcdef", "１２３４５６"})
  void malformedCodesFailBeforeRedis(String code) {
    assertInvalid(() -> service.check(account(null, "hash"), code));
    verifyNoInteractions(otp, mail, audit);
  }

  @Test
  void secretsDoNotAppearInDiagnostics() {
    assertThat(new ResetPasswordCommand("qa@example.test", "123456", "SecretPass123").toString())
        .doesNotContain("123456", "SecretPass123", "qa@example.test");
    assertThat(new VerifyPasswordResetOtpCommand("qa@example.test", "123456").toString())
        .doesNotContain("123456", "qa@example.test");
    assertThat(new PasswordResetChallenge("sensitive-digest", ISSUED).toString())
        .doesNotContain("sensitive-digest");
    assertThat(account(null, "sensitive-hash").toString()).doesNotContain("sensitive-hash");
  }

  private static PasswordResetAccount account(Instant changed, String hash) {
    return new PasswordResetAccount(
        new UserAccount(
            UUID.randomUUID(),
            hash,
            "QA",
            UserStatus.ACTIVE,
            false,
            "STUDENT",
            BaseCategory.STUDENT),
        "qa@example.test",
        changed,
        ISSUED);
  }

  private static void assertInvalid(
      org.assertj.core.api.ThrowableAssert.ThrowingCallable operation) {
    assertThatThrownBy(operation)
        .isInstanceOfSatisfying(
            DomainRuleViolationException.class,
            error -> assertThat(error.errorCode()).isEqualTo("IDT-107"))
        .hasMessage("Reset code is invalid or unavailable");
  }
}
