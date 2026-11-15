package com.algoprep.judge.application.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.algoprep.judge.application.dto.QueuePauseChange;
import com.algoprep.judge.application.dto.QueueSettingsChange;
import com.algoprep.judge.application.ports.in.ChangeQueuePauseUseCase;
import com.algoprep.judge.domain.exception.AdminConfigException;
import com.algoprep.judge.domain.model.AdminQueueSettings;
import com.algoprep.judge.domain.model.AdminQueueState;
import com.algoprep.judge.domain.model.JudgeAuditEntry;
import com.algoprep.judge.domain.ports.out.AdminQueueRepository;
import com.algoprep.judge.domain.ports.out.JudgeAuditPort;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ChangeAdminQueueCommandHandlerTest {
  private final AdminQueueRepository repository = mock(AdminQueueRepository.class);
  private final ChangeQueuePauseUseCase pause = mock(ChangeQueuePauseUseCase.class);
  private final JudgeAuditPort audit = mock(JudgeAuditPort.class);
  private final ChangeAdminQueueCommandHandler handler =
      new ChangeAdminQueueCommandHandler(repository, pause, audit);
  private final UUID actor = UUID.randomUUID();
  private static final Instant NOW = Instant.parse("2026-11-05T01:00:00Z");

  private static AdminQueueState state(boolean paused, boolean enabled) {
    return new AdminQueueState(paused, enabled, new AdminQueueSettings(20, 20, 24, 10, NOW));
  }

  private JudgeAuditEntry entry() {
    var captor = ArgumentCaptor.forClass(JudgeAuditEntry.class);
    verify(audit).record(captor.capture());
    assertThat(captor.getValue().actorUserId()).isEqualTo(actor);
    assertThat(captor.getValue().targetType()).isEqualTo("CLUSTER_SETTINGS");
    assertThat(captor.getValue().targetId()).isEqualTo("1");
    return captor.getValue();
  }

  @Test
  void pauseWrapsTheExistingKernelAndAuditsTheStoredResult() {
    when(repository.lock()).thenReturn(state(false, true));
    when(pause.change(false, true)).thenReturn(QueuePauseChange.CHANGED);
    when(repository.read()).thenReturn(state(true, true));
    assertThat(handler.setPaused(actor, false, true).value()).isTrue();
    verify(repository).markPauseActor(actor);
    var entry = entry();
    assertThat(entry.actionType()).isEqualTo("QUEUE_PAUSE_CHANGED");
    assertThat(entry.beforeJson()).isEqualTo("{\"queuePaused\":false}");
    assertThat(entry.afterJson()).isEqualTo("{\"queuePaused\":true}");
  }

  @Test
  void pauseConflictAndNoOpNeverWriteActorOrAudit() {
    when(repository.lock()).thenReturn(state(true, true));
    when(pause.change(false, true)).thenReturn(QueuePauseChange.CONFLICT);
    assertThatThrownBy(() -> handler.setPaused(actor, false, true))
        .isInstanceOfSatisfying(
            AdminConfigException.class, e -> assertThat(e.errorCode()).isEqualTo("JDG-201"));
    when(pause.change(true, true)).thenReturn(QueuePauseChange.UNCHANGED);
    assertThat(handler.setPaused(actor, true, true).updatedAt()).isEqualTo(NOW);
    verify(repository, never()).markPauseActor(any());
    verify(audit, never()).record(any());
  }

  @Test
  void autoscaleRequiresExpectedStateAndSkipsNoOpAudit() {
    when(repository.lock()).thenReturn(state(false, true));
    assertThatThrownBy(() -> handler.setAutoscale(actor, false, false))
        .isInstanceOfSatisfying(
            AdminConfigException.class, e -> assertThat(e.errorCode()).isEqualTo("JDG-201"));
    assertThat(handler.setAutoscale(actor, true, true).updatedAt()).isEqualTo(NOW);
    verify(repository, never()).setAutoscale(anyBoolean(), any());
    verify(audit, never()).record(any());
  }

  @Test
  void changedAutoscaleIsAuditedWithBothValues() {
    when(repository.lock()).thenReturn(state(false, true));
    when(repository.read()).thenReturn(state(false, false));
    assertThat(handler.setAutoscale(actor, true, false).value()).isFalse();
    verify(repository).setAutoscale(false, actor);
    var entry = entry();
    assertThat(entry.actionType()).isEqualTo("QUEUE_AUTOSCALE_CHANGED");
    assertThat(entry.beforeJson()).isEqualTo("{\"autoscaleEnabled\":true}");
    assertThat(entry.afterJson()).isEqualTo("{\"autoscaleEnabled\":false}");
  }

  @Test
  void settingsWriteAndAuditOnlyTheChangedFields() {
    var stored = state(false, true);
    when(repository.lock()).thenReturn(stored);
    when(repository.read()).thenReturn(stored);
    handler.update(actor, new QueueSettingsChange(20, 7, 100000, 10));
    verify(repository).saveSettings(Map.of("jobsPerPage", 7, "errorRetentionHours", 100000), actor);
    var entry = entry();
    assertThat(entry.actionType()).isEqualTo("QUEUE_SETTINGS_CHANGED");
    assertThat(entry.beforeJson())
        .contains("\"jobsPerPage\":20", "\"errorRetentionHours\":24")
        .doesNotContain("updatedAt", "autoscaleJobThreshold", "latencyCapSeconds");
    assertThat(entry.afterJson()).contains("\"jobsPerPage\":7", "\"errorRetentionHours\":100000");
  }

  @Test
  void unchangedSettingsKeepTheTimestampAndDoNotAudit() {
    var stored = state(false, true);
    when(repository.lock()).thenReturn(stored);
    assertThat(handler.update(actor, new QueueSettingsChange(20, 20, 24, 10)))
        .isEqualTo(stored.settings());
    verify(repository, never()).saveSettings(any(), any());
    verify(audit, never()).record(any());
  }

  @Test
  void auditFailureEscapesForAllThreeWrites() {
    when(repository.lock()).thenReturn(state(false, true));
    when(pause.change(false, true)).thenReturn(QueuePauseChange.CHANGED);
    doThrow(new IllegalStateException("Audit unavailable")).when(audit).record(any());
    assertThatThrownBy(() -> handler.setPaused(actor, false, true))
        .isInstanceOf(IllegalStateException.class);
    assertThatThrownBy(() -> handler.setAutoscale(actor, true, false))
        .isInstanceOf(IllegalStateException.class);
    assertThatThrownBy(() -> handler.update(actor, new QueueSettingsChange(1, 1, 1, 1)))
        .isInstanceOf(IllegalStateException.class);
    verify(repository, never()).read();
  }
}
