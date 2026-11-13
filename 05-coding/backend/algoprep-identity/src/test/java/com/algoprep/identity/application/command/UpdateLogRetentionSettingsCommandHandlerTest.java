package com.algoprep.identity.application.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.algoprep.common.exception.AlgoPrepException;
import com.algoprep.identity.application.dto.UpdateLogRetentionCommand;
import com.algoprep.identity.domain.model.AuditEntry;
import com.algoprep.identity.domain.model.ExpiredLogPolicy;
import com.algoprep.identity.domain.model.LogRetentionSettings;
import com.algoprep.identity.domain.ports.out.AuditLogPort;
import com.algoprep.identity.domain.ports.out.LogRetentionRepository;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/** #52: validation, both keys written, one audit row with the old and new values. */
class UpdateLogRetentionSettingsCommandHandlerTest {
  private static final UUID ACTOR = UUID.randomUUID();
  private final LogRetentionRepository retention = mock(LogRetentionRepository.class);
  private final AuditLogPort audit = mock(AuditLogPort.class);
  private final UpdateLogRetentionSettingsCommandHandler handler =
      new UpdateLogRetentionSettingsCommandHandler(retention, audit);

  private static UpdateLogRetentionCommand cmd(Long days, String policy) {
    return new UpdateLogRetentionCommand(ACTOR, days, policy);
  }

  private static void assertInvalid(Throwable error) {
    assertThat(error)
        .isInstanceOfSatisfying(
            AlgoPrepException.class, e -> assertThat(e.errorCode()).isEqualTo("IDT-442"));
  }

  @Test
  void storesBothValuesAndAuditsTheOldAndNewOnes() {
    when(retention.readForUpdate())
        .thenReturn(new LogRetentionSettings(90, ExpiredLogPolicy.DELETE));

    LogRetentionSettings saved = handler.update(cmd(30L, "ARCHIVE"));

    assertThat(saved).isEqualTo(new LogRetentionSettings(30, ExpiredLogPolicy.ARCHIVE));
    verify(retention).save(saved, ACTOR);
    ArgumentCaptor<AuditEntry> entry = ArgumentCaptor.forClass(AuditEntry.class);
    verify(audit).record(entry.capture());
    assertThat(entry.getValue().actorUserId()).isEqualTo(ACTOR);
    assertThat(entry.getValue().actionType()).isEqualTo("AUDIT_LOG_SETTINGS_CHANGED");
    assertThat(entry.getValue().beforeJson())
        .isEqualTo("{\"retentionDays\":90,\"expiredPolicy\":\"DELETE\"}");
    assertThat(entry.getValue().afterJson())
        .isEqualTo("{\"retentionDays\":30,\"expiredPolicy\":\"ARCHIVE\"}");
  }

  @Test
  void oneDayIsTheSmallestAcceptedValueAndTheUpperBoundIsAccepted() {
    when(retention.readForUpdate())
        .thenReturn(new LogRetentionSettings(90, ExpiredLogPolicy.DELETE));

    assertThat(handler.update(cmd(1L, "DELETE")).retentionDays()).isEqualTo(1);
    assertThat(handler.update(cmd(1_000_000L, "DELETE")).retentionDays()).isEqualTo(1_000_000);
  }

  @Test
  void invalidDaysAreRefusedWithIdt442AndNothingIsWritten() {
    Long[] bad = {0L, -1L, null, 1_000_001L, Long.MAX_VALUE, (long) Integer.MIN_VALUE};
    for (Long days : bad) {
      assertThatThrownBy(() -> handler.update(cmd(days, "DELETE")))
          .satisfies(UpdateLogRetentionSettingsCommandHandlerTest::assertInvalid);
    }
    verify(retention, never()).save(any(), any());
    verify(audit, never()).record(any());
  }

  @Test
  void unknownOrMissingPolicyIsRefusedWithIdt442() {
    String[] bad = {null, "", "delete", "KEEP", "DELETE "};
    for (String policy : bad) {
      assertThatThrownBy(() -> handler.update(cmd(30L, policy)))
          .satisfies(UpdateLogRetentionSettingsCommandHandlerTest::assertInvalid);
    }
    verify(retention, never()).save(any(), any());
    verify(audit, never()).record(any());
  }
}
