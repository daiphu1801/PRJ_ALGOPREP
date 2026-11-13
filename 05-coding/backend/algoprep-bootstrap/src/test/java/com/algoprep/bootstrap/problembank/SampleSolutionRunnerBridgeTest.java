package com.algoprep.bootstrap.problembank;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.algoprep.judge.application.dto.ExecutionModels.EvaluationRequest;
import com.algoprep.judge.application.dto.ExecutionModels.EvaluationResult;
import com.algoprep.judge.application.dto.ExecutionModels.TestcaseResult;
import com.algoprep.judge.application.dto.ExecutionModels.Verdict;
import com.algoprep.judge.application.dto.SampleRun;
import com.algoprep.judge.application.dto.WorkerPreparation.Prepared;
import com.algoprep.judge.application.ports.in.EvaluateProgramUseCase;
import com.algoprep.judge.application.ports.in.GetLanguageMultipliersUseCase;
import com.algoprep.judge.application.ports.out.SampleProgramPort;
import com.algoprep.judge.domain.model.LanguageMultipliers;
import com.algoprep.problembank.application.dto.ReferenceSolutionRun;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/** The mapping the bridge owns; the harness and the sandbox behind it have their own tests. */
class SampleSolutionRunnerBridgeTest {
  private final SampleProgramPort programs = mock(SampleProgramPort.class);
  private final EvaluateProgramUseCase evaluation = mock(EvaluateProgramUseCase.class);
  private final GetLanguageMultipliersUseCase multipliers =
      mock(GetLanguageMultipliersUseCase.class);
  private final SampleSolutionRunnerBridge bridge =
      new SampleSolutionRunnerBridge(programs, evaluation, multipliers);

  private final UUID problem = UUID.randomUUID();
  private final UUID first = UUID.randomUUID();
  private final UUID second = UUID.randomUUID();

  private ReferenceSolutionRun.Request request(boolean wrapper) {
    return new ReferenceSolutionRun.Request(
        problem,
        "PYTHON",
        wrapper,
        "def solve(): pass",
        4,
        new ReferenceSolutionRun.Limits(1000, 256, 64, 10),
        List.of(
            new ReferenceSolutionRun.Case(first, "1", "1"),
            new ReferenceSolutionRun.Case(second, "2", "2")));
  }

  private void prepared() {
    when(multipliers.getLanguageMultipliers("PYTHON"))
        .thenReturn(new LanguageMultipliers(new BigDecimal("3"), new BigDecimal("1.5")));
    when(programs.prepare(any())).thenReturn(mock(Prepared.class));
  }

  private static TestcaseResult row(int order, Verdict verdict) {
    return new TestcaseResult(order, verdict, 5, 100, "", "", "");
  }

  @Test
  void scalesLimitsByTheLanguageMultipliersAndChoosesTheModelFromTheProblem() {
    prepared();
    when(evaluation.evaluate(any()))
        .thenReturn(
            new EvaluationResult(
                Verdict.AC, null, null, List.of(row(1, Verdict.AC), row(2, Verdict.AC))));

    bridge.run(request(true));

    var sent = ArgumentCaptor.forClass(EvaluationRequest.class);
    verify(evaluation).evaluate(sent.capture());
    var limits = sent.getValue().testcases().get(0).limits();
    assertThat(limits.timeMs()).isEqualTo(3000); // 1000 ms x 3
    assertThat(limits.memoryBytes()).isEqualTo(384L * 1_048_576L); // 256 MB x 1.5
    assertThat(limits.outputBytes()).isEqualTo(64L * 1024);
    assertThat(sent.getValue().compileLimits().timeMs()).isEqualTo(10_000);
    assertThat(sent.getValue().testcases()).extracting(t -> t.stdin()).containsExactly("1", "2");
    var preparation = ArgumentCaptor.forClass(SampleRun.Preparation.class);
    verify(programs).prepare(preparation.capture());
    assertThat(preparation.getValue().command().submissionModel()).isEqualTo("FUNCTION_WRAPPER");
    assertThat(preparation.getValue().specVersion()).isEqualTo(4);
  }

  @Test
  void aProblemWithoutAWrapperRunsAsStandardIo() {
    prepared();
    when(evaluation.evaluate(any()))
        .thenReturn(
            new EvaluationResult(
                Verdict.AC, null, null, List.of(row(1, Verdict.AC), row(2, Verdict.AC))));

    bridge.run(request(false));

    var preparation = ArgumentCaptor.forClass(SampleRun.Preparation.class);
    verify(programs).prepare(preparation.capture());
    assertThat(preparation.getValue().command().submissionModel()).isEqualTo("STANDARD_IO");
  }

  @Test
  void mapsEachCaseVerdictAndAllPassedNeedsEveryCase() {
    prepared();
    when(evaluation.evaluate(any()))
        .thenReturn(
            new EvaluationResult(
                Verdict.WA, null, null, List.of(row(1, Verdict.AC), row(2, Verdict.WA))));

    var result = bridge.run(request(true));

    assertThat(result.compileError()).isFalse();
    assertThat(result.results())
        .extracting(r -> r.testcaseId(), r -> r.passed(), r -> r.verdict())
        .containsExactly(
            org.assertj.core.groups.Tuple.tuple(first, true, "AC"),
            org.assertj.core.groups.Tuple.tuple(second, false, "WA"));
    assertThat(result.allPassed()).isFalse();
  }

  @Test
  void aCompileErrorRunsNoCaseAndNeverCountsAsPassing() {
    prepared();
    when(evaluation.evaluate(any()))
        .thenReturn(new EvaluationResult(Verdict.COMPILE_ERROR, null, "boom", List.of()));

    var result = bridge.run(request(true));

    assertThat(result.compileError()).isTrue();
    assertThat(result.results()).isEmpty();
    assertThat(result.allPassed()).isFalse();
  }

  @Test
  void aSystemErrorIsAnExceptionSoThePublishAnswers503() {
    prepared();
    when(evaluation.evaluate(any()))
        .thenReturn(new EvaluationResult(Verdict.SYSTEM_ERROR, "down", null, List.of()));

    assertThatThrownBy(() -> bridge.run(request(true))).isInstanceOf(IllegalStateException.class);
  }
}
