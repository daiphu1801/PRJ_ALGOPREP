package com.algoprep.problembank.application.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.algoprep.common.exception.AlgoPrepException;
import com.algoprep.problembank.application.command.GeneratedTestcaseWriter.Kept;
import com.algoprep.problembank.application.command.GeneratedTestcaseWriter.RunInfo;
import com.algoprep.problembank.application.dto.AuthoringScope;
import com.algoprep.problembank.application.dto.TestcaseGeneration;
import com.algoprep.problembank.domain.model.ApprovalCounts;
import com.algoprep.problembank.domain.model.ProblemContentSnapshot;
import com.algoprep.problembank.domain.model.ProblemTouch;
import com.algoprep.problembank.domain.model.StoredGradingTestcase.Content;
import com.algoprep.problembank.domain.model.StoredTestcase;
import com.algoprep.problembank.domain.model.TestcaseGenerationRun;
import com.algoprep.problembank.domain.ports.out.TestcaseGenerationRepository;
import com.algoprep.problembank.domain.ports.out.TestcaseWriteRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class GeneratedTestcaseWriterTest {
  private final TestcaseAuthoringSupport support = mock(TestcaseAuthoringSupport.class);
  private final TestcaseWriteRepository testcases = mock(TestcaseWriteRepository.class);
  private final TestcaseContentStore contents = mock(TestcaseContentStore.class);
  private final TestcaseGenerationRepository generation = mock(TestcaseGenerationRepository.class);
  private final GeneratedTestcaseWriter writer =
      new GeneratedTestcaseWriter(support, testcases, contents, generation);
  private final UUID actor = UUID.randomUUID();
  private final UUID problemId = UUID.randomUUID();
  private final AuthoringScope scope = new AuthoringScope(actor, false);
  private final ProblemContentSnapshot problem = mock(ProblemContentSnapshot.class);

  @BeforeEach
  void setup() {
    when(support.lock(scope, problemId)).thenReturn(problem);
    when(generation.consumeQuota(problemId)).thenReturn(true);
    when(testcases.counts(problemId)).thenReturn(new ApprovalCounts(3, 3, 2));
    when(testcases.nextPosition(problemId)).thenReturn(4);
    when(contents.newTestcaseId()).thenAnswer(c -> UUID.randomUUID());
    when(contents.place(any(), any(), any(), any()))
        .thenAnswer(c -> new Content("INLINE", c.getArgument(3), null));
    when(support.settle(problem, actor, false))
        .thenReturn(new ProblemTouch(Instant.parse("2026-11-10T10:00:00Z"), 1));
  }

  private RunInfo info() {
    return new RunInfo(
        actor,
        new TestcaseGeneration.Scripts("gen", "val", List.of(), "model-y", "prompt-1"),
        99L,
        List.of(new TestcaseGeneration.Dropped("DUPLICATE_INPUT", 1)),
        0,
        2);
  }

  @Test
  void storesEveryPairAsAnUnapprovedHiddenAiDraftAndKeepsTheGradingSetUntouched() {
    var saved =
        writer.persist(
            scope,
            problemId,
            List.of(new Kept("TYPICAL", "1\n", "2\n"), new Kept(null, "3\n", "4\n")),
            info());

    var rows = ArgumentCaptor.forClass(StoredTestcase.class);
    verify(testcases, org.mockito.Mockito.times(2)).insert(any(), rows.capture());
    assertThat(rows.getAllValues())
        .allSatisfy(
            row -> {
              assertThat(row.origin()).isEqualTo("AI");
              assertThat(row.approved()).isFalse();
              assertThat(row.visibility()).isEqualTo("HIDDEN");
            });
    assertThat(rows.getAllValues()).extracting(StoredTestcase::position).containsExactly(4, 5);
    assertThat(rows.getAllValues().get(0).category()).isEqualTo("TYPICAL");
    verify(support).settle(problem, actor, false);
    assertThat(saved.testcases()).hasSize(2);
    var run = ArgumentCaptor.forClass(TestcaseGenerationRun.class);
    verify(generation).insertRun(run.capture());
    assertThat(run.getValue().addedCount()).isEqualTo(2);
    assertThat(run.getValue().seed()).isEqualTo(99L);
    assertThat(run.getValue().droppedJson()).contains("DUPLICATE_INPUT");
    assertThat(run.getValue().requestedBy()).isEqualTo(actor);
  }

  @Test
  void aReachedCapIsPrb042AndNothingIsWritten() {
    when(generation.consumeQuota(problemId)).thenReturn(false);

    assertThatThrownBy(
            () -> writer.persist(scope, problemId, List.of(new Kept(null, "1\n", "2\n")), info()))
        .isInstanceOfSatisfying(
            AlgoPrepException.class, e -> assertThat(e.errorCode()).isEqualTo("PRB-042"));
    verify(testcases, never()).insert(any(), any());
    verify(generation, never()).insertRun(any());
  }

  @Test
  void moreThanFiveHundredTestcasesInTotalIsPrb046() {
    when(testcases.counts(problemId)).thenReturn(new ApprovalCounts(499, 499, 2));

    assertThatThrownBy(
            () ->
                writer.persist(
                    scope,
                    problemId,
                    List.of(new Kept(null, "1\n", "2\n"), new Kept(null, "3\n", "4\n")),
                    info()))
        .isInstanceOfSatisfying(
            AlgoPrepException.class, e -> assertThat(e.errorCode()).isEqualTo("PRB-046"));
    verify(generation, never()).insertRun(any());
  }
}
