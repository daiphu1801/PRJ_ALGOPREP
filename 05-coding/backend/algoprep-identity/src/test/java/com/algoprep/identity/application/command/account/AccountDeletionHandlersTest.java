package com.algoprep.identity.application.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.algoprep.common.exception.AlgoPrepException;
import com.algoprep.common.exception.UnauthenticatedException;
import com.algoprep.identity.domain.model.AuditEntry;
import com.algoprep.identity.domain.model.MyProfile;
import com.algoprep.identity.domain.ports.out.AccountLifecycleRepository;
import com.algoprep.identity.domain.ports.out.AuditLogPort;
import com.algoprep.identity.domain.ports.out.MyProfileRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/** BR-08 request side (#18) and the batching of JOB-ID-02. */
class AccountDeletionHandlersTest {
  private final MyProfileRepository profiles = mock(MyProfileRepository.class);
  private final AccountLifecycleRepository accounts = mock(AccountLifecycleRepository.class);
  private final AuditLogPort audit = mock(AuditLogPort.class);
  private final DeleteMyAccountCommandHandler delete =
      new DeleteMyAccountCommandHandler(profiles, accounts, audit);
  private final AnonymizeDeletedAccountsCommandHandler anonymize =
      new AnonymizeDeletedAccountsCommandHandler(accounts);
  private final UUID userId = UUID.randomUUID();

  AccountDeletionHandlersTest() {
    MyProfile me =
        new MyProfile(
            userId,
            "Name",
            "a@example.test",
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

  private static void assertCode(Throwable error, String code) {
    assertThat(error)
        .isInstanceOfSatisfying(
            AlgoPrepException.class, e -> assertThat(e.errorCode()).isEqualTo(code));
  }

  @Test
  void theTypedEmailIgnoringCaseAndSurroundingSpacesDeactivatesTheAccountAndAudits() {
    when(accounts.deactivateSelf(userId)).thenReturn(true);

    delete.delete(userId, "  A@Example.TEST ");

    ArgumentCaptor<AuditEntry> entry = ArgumentCaptor.forClass(AuditEntry.class);
    verify(audit).record(entry.capture());
    assertThat(entry.getValue().actionType()).isEqualTo("SELF_ACCOUNT_DELETE_REQUESTED");
    assertThat(entry.getValue().beforeJson()).isNull();
    assertThat(entry.getValue().afterJson()).isNull();
  }

  @Test
  void aDifferentOrMissingEmailIs422AndNothingChanges() {
    for (String typed : new String[] {"b@example.test", "a@example.tes", "", null}) {
      assertThatThrownBy(() -> delete.delete(userId, typed))
          .satisfies(e -> assertCode(e, "IDT-201"));
    }

    verify(accounts, never()).deactivateSelf(any());
    verify(audit, never()).record(any());
  }

  @Test
  void theSoleActiveAdminIs422IDT432AndNothingChanges() {
    when(accounts.isLastActiveAdmin(userId)).thenReturn(true);

    assertThatThrownBy(() -> delete.delete(userId, "a@example.test"))
        .satisfies(e -> assertCode(e, "IDT-432"));

    verify(accounts, never()).deactivateSelf(any());
    verify(audit, never()).record(any());
  }

  @Test
  void aWrongEmailIsReportedBeforeTheLastAdminCheckSoNothingIsLocked() {
    assertThatThrownBy(() -> delete.delete(userId, "other@example.test"))
        .satisfies(e -> assertCode(e, "IDT-201"));

    verify(accounts, never()).isLastActiveAdmin(any());
  }

  @Test
  void anAccountThatIsNotActiveAnymoreIs401AndNothingIsAudited() {
    when(accounts.deactivateSelf(userId)).thenReturn(false);

    assertThatThrownBy(() -> delete.delete(userId, "a@example.test"))
        .isInstanceOf(UnauthenticatedException.class);
    verify(audit, never()).record(any());
  }

  @Test
  void anonymisationStopsAtTheFirstShortBatch() {
    when(accounts.anonymizeDueBatch(anyInt())).thenReturn(200, 37);

    assertThat(anonymize.anonymize()).isEqualTo(237);
    verify(accounts, times(2)).anonymizeDueBatch(200);
  }

  @Test
  void anonymisationRunsAtMostFiveBatchesPerRun() {
    when(accounts.anonymizeDueBatch(anyInt())).thenReturn(200);

    assertThat(anonymize.anonymize()).isEqualTo(1000);
    verify(accounts, times(5)).anonymizeDueBatch(200);
  }

  @Test
  void anonymisationWithNothingDueDoesNothing() {
    when(accounts.anonymizeDueBatch(anyInt())).thenReturn(0);

    assertThat(anonymize.anonymize()).isZero();
    verify(accounts, times(1)).anonymizeDueBatch(200);
  }
}
