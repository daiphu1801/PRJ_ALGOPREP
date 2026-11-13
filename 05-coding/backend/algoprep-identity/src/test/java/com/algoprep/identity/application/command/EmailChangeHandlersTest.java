package com.algoprep.identity.application.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.algoprep.common.exception.AlgoPrepException;
import com.algoprep.identity.application.dto.ConfirmEmailChangeCommand;
import com.algoprep.identity.application.dto.EmailChangeIssueResult;
import com.algoprep.identity.application.dto.PendingEmailChange;
import com.algoprep.identity.application.dto.RequestEmailChangeCommand;
import com.algoprep.identity.application.ports.out.EmailChangeOtpPort;
import com.algoprep.identity.application.ports.out.EmailPort;
import com.algoprep.identity.domain.model.AuditEntry;
import com.algoprep.identity.domain.model.MyProfile;
import com.algoprep.identity.domain.ports.out.AuditLogPort;
import com.algoprep.identity.domain.ports.out.MyProfileRepository;
import jakarta.validation.Validation;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** #11 and #12 against mocked ports; the Redis and mail path is covered by MyAccountFlowTest. */
class EmailChangeHandlersTest {
  private final MyProfileRepository profiles = mock(MyProfileRepository.class);
  private final EmailChangeOtpPort otp = mock(EmailChangeOtpPort.class);
  private final EmailPort mail = mock(EmailPort.class);
  private final AuditLogPort audit = mock(AuditLogPort.class);
  private final EmailChangeService service = new EmailChangeService(otp, mail);
  private final RequestEmailChangeCommandHandler request =
      new RequestEmailChangeCommandHandler(
          profiles,
          new RegistrationValidation(Validation.buildDefaultValidatorFactory().getValidator()),
          service);
  private final ConfirmEmailChangeCommandHandler confirm =
      new ConfirmEmailChangeCommandHandler(profiles, service, audit);
  private final UUID userId = UUID.randomUUID();

  @BeforeEach
  void startTransaction() {
    TransactionSynchronizationManager.initSynchronization();
    MyProfile me =
        new MyProfile(
            userId,
            "Name",
            "old@example.test",
            null,
            null,
            null,
            "PYTHON",
            null,
            true,
            null,
            List.of(),
            Instant.parse("2026-10-31T00:00:00Z"),
            "Học viên",
            false);
    when(profiles.findById(userId)).thenReturn(Optional.of(me));
  }

  @AfterEach
  void endTransaction() {
    TransactionSynchronizationManager.clearSynchronization();
  }

  private RequestEmailChangeCommand ask(String email) {
    return new RequestEmailChangeCommand(userId, email);
  }

  private static void assertCode(Throwable error, String code) {
    assertThat(error)
        .isInstanceOfSatisfying(
            AlgoPrepException.class, e -> assertThat(e.errorCode()).isEqualTo(code));
  }

  @Test
  void aFreeAddressGetsACodeByMailAndTheLoginEmailIsNotTouched() {
    when(otp.issue(any(), anyString(), anyString())).thenReturn(EmailChangeIssueResult.ISSUED);

    PendingEmailChange result = request.request(ask("  New@Example.Test "));

    assertThat(result).isEqualTo(new PendingEmailChange("new@example.test", 600));
    ArgumentCaptor<String> body = ArgumentCaptor.forClass(String.class);
    verify(mail)
        .send(org.mockito.ArgumentMatchers.eq("new@example.test"), anyString(), body.capture());
    Matcher code = Pattern.compile("\\b[0-9]{6}\\b").matcher(body.getValue());
    assertThat(code.find()).isTrue();
    verify(profiles, never()).updateEmail(any(), anyString());
  }

  @Test
  void anInvalidAddressIs400BeforeAnythingElse() {
    assertThatThrownBy(() -> request.request(ask("not-an-email")))
        .satisfies(e -> assertCode(e, "IDT-400"));
    verify(otp, never()).issue(any(), anyString(), anyString());
  }

  @Test
  void theCurrentOrAnAlreadyUsedAddressIs409WithoutIssuingACode() {
    assertThatThrownBy(() -> request.request(ask("OLD@example.test")))
        .satisfies(e -> assertCode(e, "IDT-415"));
    when(profiles.emailTaken("taken@example.test")).thenReturn(true);
    assertThatThrownBy(() -> request.request(ask("taken@example.test")))
        .satisfies(e -> assertCode(e, "IDT-415"));

    verify(otp, never()).issue(any(), anyString(), anyString());
    verify(mail, never()).send(anyString(), anyString(), anyString());
  }

  @Test
  void cooldownAndBudgetAre429WithTheirOwnCodesAndSendNothing() {
    when(otp.issue(any(), anyString(), anyString())).thenReturn(EmailChangeIssueResult.COOLDOWN);
    assertThatThrownBy(() -> request.request(ask("new@example.test")))
        .satisfies(e -> assertCode(e, "IDT-V12"));

    when(otp.issue(any(), anyString(), anyString()))
        .thenReturn(EmailChangeIssueResult.BUDGET_EXCEEDED);
    assertThatThrownBy(() -> request.request(ask("new@example.test")))
        .satisfies(e -> assertCode(e, "IDT-V05"));

    verify(mail, never()).send(anyString(), anyString(), anyString());
  }

  @Test
  void aMatchingCodeMovesTheEmailAndAuditsWithoutPersonalData() {
    when(otp.match(any(), anyString())).thenReturn(Optional.of("new@example.test"));
    when(profiles.updateEmail(userId, "new@example.test")).thenReturn(true);

    String email = confirm.confirm(new ConfirmEmailChangeCommand(userId, "123456"));

    assertThat(email).isEqualTo("new@example.test");
    ArgumentCaptor<AuditEntry> entry = ArgumentCaptor.forClass(AuditEntry.class);
    verify(audit).record(entry.capture());
    assertThat(entry.getValue().actionType()).isEqualTo("EMAIL_CHANGED");
    assertThat(entry.getValue().beforeJson()).isNull();
    assertThat(entry.getValue().afterJson()).isNull();
  }

  @Test
  void aWrongMalformedOrMissingCodeIsTheSame422() {
    when(otp.match(any(), anyString())).thenReturn(Optional.empty());

    for (String code : new String[] {"000000", "12345", "abcdef", null}) {
      assertThatThrownBy(() -> confirm.confirm(new ConfirmEmailChangeCommand(userId, code)))
          .satisfies(e -> assertCode(e, "IDT-416"));
    }

    verify(profiles, never()).updateEmail(any(), anyString());
    verify(audit, never()).record(any());
  }

  @Test
  void anAddressTakenInTheMeantimeIs409AndNothingIsAudited() {
    when(otp.match(any(), anyString())).thenReturn(Optional.of("new@example.test"));
    when(profiles.updateEmail(userId, "new@example.test")).thenReturn(false);

    assertThatThrownBy(() -> confirm.confirm(new ConfirmEmailChangeCommand(userId, "123456")))
        .satisfies(e -> assertCode(e, "IDT-415"));

    verify(audit, never()).record(any());
  }
}
