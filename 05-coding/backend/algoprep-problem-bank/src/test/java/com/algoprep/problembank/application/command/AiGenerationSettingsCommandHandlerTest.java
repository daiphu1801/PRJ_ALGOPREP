package com.algoprep.problembank.application.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.algoprep.common.exception.AlgoPrepException;
import com.algoprep.problembank.application.dto.TestcaseGeneration.UpdateSettings;
import com.algoprep.problembank.domain.model.CatalogAuditEntry;
import com.algoprep.problembank.domain.ports.out.ProblemAuditPort;
import com.algoprep.problembank.domain.ports.out.ProblemAuthoringReadRepository;
import com.algoprep.problembank.domain.ports.out.TestcaseGenerationRepository;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class AiGenerationSettingsCommandHandlerTest {
  private final ProblemAuthoringReadRepository reads = mock(ProblemAuthoringReadRepository.class);
  private final TestcaseGenerationRepository repository = mock(TestcaseGenerationRepository.class);
  private final ProblemAuditPort audit = mock(ProblemAuditPort.class);
  private final AiGenerationSettingsCommandHandler handler =
      new AiGenerationSettingsCommandHandler(reads, repository, audit);
  private final UUID actor = UUID.randomUUID();

  @Test
  void getReadsTheStoredCap() {
    when(reads.aiGenerationLimit()).thenReturn(2);

    assertThat(handler.get().maxPerProblem()).isEqualTo(2);
  }

  @Test
  void aChangeWritesTheCapAndExactlyOneAuditRowWithOldAndNewValues() {
    when(repository.lockCap()).thenReturn(2);

    var result = handler.update(new UpdateSettings(actor, 5));

    assertThat(result.maxPerProblem()).isEqualTo(5);
    verify(repository).updateCap(5, actor);
    var entry = ArgumentCaptor.forClass(CatalogAuditEntry.class);
    verify(audit).record(entry.capture());
    assertThat(entry.getValue().actionType()).isEqualTo("AI_TESTCASE_GENERATION_LIMIT_CHANGED");
    assertThat(entry.getValue().targetType()).isEqualTo("PROBLEM_BANK_SETTINGS");
    assertThat(entry.getValue().actorUserId()).isEqualTo(actor);
    assertThat(entry.getValue().beforeJson()).isEqualTo("{\"maxPerProblem\":2}");
    assertThat(entry.getValue().afterJson()).isEqualTo("{\"maxPerProblem\":5}");
  }

  @Test
  void theBoundsOneAndTenPassAndZeroAndElevenAreRejectedBeforeAnyWrite() {
    when(repository.lockCap()).thenReturn(2);
    handler.update(new UpdateSettings(actor, 1));
    handler.update(new UpdateSettings(actor, 10));
    for (int bad : new int[] {0, 11, -3}) {
      assertThatThrownBy(() -> handler.update(new UpdateSettings(actor, bad)))
          .isInstanceOfSatisfying(
              AlgoPrepException.class, e -> assertThat(e.errorCode()).isEqualTo("PRB-001"));
    }
  }

  @Test
  void anUnchangedValueWritesNothing() {
    when(repository.lockCap()).thenReturn(4);

    assertThat(handler.update(new UpdateSettings(actor, 4)).maxPerProblem()).isEqualTo(4);
    verify(repository, never()).updateCap(any(Integer.class), any());
    verify(audit, never()).record(any());
  }

  @Test
  void aFailedAuditIsPrb050SoTheTransactionRollsTheCapBack() {
    when(repository.lockCap()).thenReturn(2);
    doThrow(new IllegalStateException("audit down")).when(audit).record(any());

    assertThatThrownBy(() -> handler.update(new UpdateSettings(actor, 3)))
        .isInstanceOfSatisfying(
            AlgoPrepException.class, e -> assertThat(e.errorCode()).isEqualTo("PRB-050"));
  }
}
