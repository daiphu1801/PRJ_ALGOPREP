package com.algoprep.problembank.application.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.algoprep.common.exception.ServiceUnavailableException;
import com.algoprep.problembank.application.dto.CreateProblemCommand;
import com.algoprep.problembank.application.dto.CreateProblemCommand.PartialLimits;
import com.algoprep.problembank.application.dto.JudgeGradingDefaults;
import com.algoprep.problembank.application.dto.SaveProblemContentCommand;
import com.algoprep.problembank.application.ports.in.SaveProblemContentUseCase;
import com.algoprep.problembank.application.ports.out.JudgeConfigReadPort;
import com.algoprep.problembank.domain.model.AuthoringProblemRecord;
import com.algoprep.problembank.domain.model.CatalogAuditEntry;
import com.algoprep.problembank.domain.model.ProblemContentData;
import com.algoprep.problembank.domain.model.ProblemContentSnapshot;
import com.algoprep.problembank.domain.ports.out.ProblemAuditPort;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CreateProblemCommandHandlerTest {
  private final SaveProblemContentUseCase save = mock(SaveProblemContentUseCase.class);
  private final JudgeConfigReadPort defaults = mock(JudgeConfigReadPort.class);
  private final ProblemDuplicator duplicator = mock(ProblemDuplicator.class);
  private final ProblemAuditPort audit = mock(ProblemAuditPort.class);
  private final CreateProblemCommandHandler handler =
      new CreateProblemCommandHandler(save, defaults, duplicator, audit);
  private final UUID actor = UUID.randomUUID();
  private final UUID created = UUID.randomUUID();

  private ProblemContentData content() {
    return new ProblemContentData(
        "Two Sum",
        "statement",
        "",
        null,
        new ProblemContentData.Limits(0, 0, 0, null),
        List.of(),
        List.of(),
        List.of(),
        null);
  }

  private SaveProblemContentCommand run(PartialLimits limits, boolean admin) {
    when(defaults.getGradingDefaults()).thenReturn(new JudgeGradingDefaults(2000, 256, 64, 10));
    handler.create(new CreateProblemCommand(actor, admin, content(), limits));
    var captor = forClass(SaveProblemContentCommand.class);
    verify(save).save(captor.capture());
    return captor.getValue();
  }

  @Test
  void missingLimitsTakeTheGradingDefaultsAndTheHourlyCapStaysNull() {
    var sent = run(new PartialLimits(null, null, null, null), false);

    var limits = sent.content().limits();
    assertThat(limits.timeLimitMs()).isEqualTo(2000);
    assertThat(limits.memoryLimitMb()).isEqualTo(256);
    assertThat(limits.maxOutputSizeKb()).isEqualTo(64);
    assertThat(limits.maxSubmissionsPerHour()).isNull(); // the system default is read at check time
  }

  @Test
  void typedLimitsWinOverTheDefaults() {
    var sent = run(new PartialLimits(500, null, 8, 30), false);

    var limits = sent.content().limits();
    assertThat(limits.timeLimitMs()).isEqualTo(500);
    assertThat(limits.memoryLimitMb()).isEqualTo(256);
    assertThat(limits.maxOutputSizeKb()).isEqualTo(8);
    assertThat(limits.maxSubmissionsPerHour()).isEqualTo(30);
  }

  @Test
  void theCoreIsCalledAsACreateByTheCallerWithTheGrantedCell() {
    var sent = run(new PartialLimits(null, null, null, null), true);

    assertThat(sent.problemId()).isNull();
    assertThat(sent.actorId()).isEqualTo(actor);
    assertThat(sent.admin()).isTrue();
    assertThat(sent.actionGranted()).isTrue();
    assertThat(sent.expectedUpdatedAt()).isNull();
    assertThat(sent.content().title()).isEqualTo("Two Sum");
  }

  @Test
  void anOrdinaryCreateTouchesNeitherTheDuplicatorNorTheAudit() {
    run(new PartialLimits(null, null, null, null), false);

    verify(duplicator, never()).source(any(), any(), org.mockito.ArgumentMatchers.anyBoolean());
    verify(audit, never()).record(any());
  }

  private AuthoringProblemRecord sourceRecord(UUID id) {
    return new AuthoringProblemRecord(
        id,
        "src",
        "Source",
        "s",
        "",
        UUID.randomUUID(),
        "UNPUBLISHED",
        true,
        new ProblemContentData.Limits(1, 1, 1, null),
        1,
        0,
        null,
        true,
        true,
        actor,
        null,
        null,
        Instant.EPOCH,
        null,
        List.of(),
        List.of(),
        null,
        List.of(),
        List.of(),
        null);
  }

  private void withSource(UUID sourceId) {
    when(defaults.getGradingDefaults()).thenReturn(new JudgeGradingDefaults(2000, 256, 64, 10));
    when(duplicator.source(sourceId, actor, false)).thenReturn(sourceRecord(sourceId));
    when(save.save(any()))
        .thenReturn(
            new ProblemContentSnapshot(
                created, "TWO-SUM", actor, "UNPUBLISHED", content(), Instant.EPOCH));
  }

  private CreateProblemCommand duplicate(UUID sourceId) {
    return new CreateProblemCommand(
        actor, false, content(), new PartialLimits(null, null, null, null), sourceId);
  }

  @Test
  void aDuplicateChecksTheSourceFirstThenCopiesAndAudits() {
    var sourceId = UUID.randomUUID();
    withSource(sourceId);

    var result = handler.create(duplicate(sourceId));

    assertThat(result.id()).isEqualTo(created);
    var order = inOrder(duplicator, save, audit);
    order.verify(duplicator).source(sourceId, actor, false);
    order.verify(save).save(any());
    order
        .verify(duplicator)
        .copy(
            any(),
            org.mockito.ArgumentMatchers.eq(created),
            org.mockito.ArgumentMatchers.eq(actor));
    var entry = forClass(CatalogAuditEntry.class);
    order.verify(audit).record(entry.capture());
    assertThat(entry.getValue().actionType()).isEqualTo("PROBLEM_DUPLICATED");
    assertThat(entry.getValue().targetType()).isEqualTo("PROBLEM");
    assertThat(entry.getValue().targetId()).isEqualTo(created.toString());
    assertThat(entry.getValue().actorUserId()).isEqualTo(actor);
    assertThat(entry.getValue().afterJson()).contains(sourceId.toString());
  }

  @Test
  void aSourceThatIsRefusedStopsBeforeAnythingIsSaved() {
    var sourceId = UUID.randomUUID();
    when(duplicator.source(sourceId, actor, false)).thenThrow(new IllegalStateException("refused"));

    assertThatThrownBy(() -> handler.create(duplicate(sourceId)))
        .isInstanceOf(IllegalStateException.class);
    verify(save, never()).save(any());
  }

  @Test
  void aLostAuditEntryIsServiceUnavailable() {
    var sourceId = UUID.randomUUID();
    withSource(sourceId);
    doThrow(new IllegalStateException("down")).when(audit).record(any());

    assertThatThrownBy(() -> handler.create(duplicate(sourceId)))
        .isInstanceOfSatisfying(
            ServiceUnavailableException.class, e -> assertThat(e.errorCode()).isEqualTo("PRB-050"));
  }
}
