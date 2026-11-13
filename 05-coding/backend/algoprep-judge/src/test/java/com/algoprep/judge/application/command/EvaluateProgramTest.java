package com.algoprep.judge.application.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.algoprep.common.exception.DomainRuleViolationException;
import com.algoprep.common.functionspec.Language;
import com.algoprep.common.functionspec.MatchingStrategy;
import com.algoprep.judge.application.dto.ExecutionModels.Compilation;
import com.algoprep.judge.application.dto.ExecutionModels.CompileReport;
import com.algoprep.judge.application.dto.ExecutionModels.EvaluationRequest;
import com.algoprep.judge.application.dto.ExecutionModels.Limits;
import com.algoprep.judge.application.dto.ExecutionModels.Match;
import com.algoprep.judge.application.dto.ExecutionModels.Matching;
import com.algoprep.judge.application.dto.ExecutionModels.Model;
import com.algoprep.judge.application.dto.ExecutionModels.Program;
import com.algoprep.judge.application.dto.ExecutionModels.RunResult;
import com.algoprep.judge.application.dto.ExecutionModels.Status;
import com.algoprep.judge.application.dto.ExecutionModels.Testcase;
import com.algoprep.judge.application.dto.ExecutionModels.Verdict;
import com.algoprep.judge.application.ports.out.HarnessEvaluationPort;
import com.algoprep.judge.application.ports.out.JudgeExecutionPort;
import com.algoprep.judge.application.ports.out.JudgeExecutionPort.CompiledExecution;
import com.algoprep.judge.domain.exception.JudgeInfrastructureException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class EvaluateProgramTest {
  private final JudgeExecutionPort engine = mock(JudgeExecutionPort.class);
  private final HarnessEvaluationPort harness = mock(HarnessEvaluationPort.class);
  private final CompiledExecution execution = mock(CompiledExecution.class);
  private final EvaluateProgram evaluator = new EvaluateProgram(engine, harness);
  private final Limits limits = new Limits(1000, 268435456, 1024, 60);

  @BeforeEach
  void executable() {
    when(engine.compile(any())).thenReturn(execution);
    when(execution.compilation()).thenReturn(new Compilation(true, ""));
    when(harness.match(any(), any(), anyString(), anyString()))
        .thenReturn(new Match(true, "result", "debug"));
  }

  private EvaluationRequest request(Model model, boolean sample, boolean stderr) {
    var program =
        new Program(Language.PYTHON, model, Map.of(), null, null, "solve(value)", "@@APH-RESULT@@");
    return new EvaluationRequest(
        program,
        limits,
        new Matching(MatchingStrategy.EXACT, null, null),
        List.of(
            new Testcase("input secret", "expected secret", sample, limits),
            new Testcase("input secret", "expected secret", sample, limits)),
        stderr);
  }

  @ParameterizedTest
  @EnumSource(
      value = Status.class,
      names = {"OK", "RE", "TLE", "MLE"})
  void continuesAfterOrdinaryTestcaseFailureAndCloses(Status status) {
    when(execution.run(any()))
        .thenReturn(new RunResult(status, "stdout secret", "stderr secret", 3, 4));
    var result = evaluator.evaluate(request(Model.FUNCTION_WRAPPER, false, true));
    assertThat(result.results()).hasSize(2);
    assertThat(result.results())
        .allSatisfy(
            row -> {
              assertThat(row.actualOutputSnippet()).isNull();
              assertThat(row.userConsole()).isNull();
              assertThat(row.stderrSnippet()).isNull();
            });
    assertThat(result.toString())
        .doesNotContain("input secret", "expected secret", "stdout secret", "stderr secret");
    verify(execution).close();
  }

  @Test
  void wrongAnswerKeepsSampleActualAndConsoleButNoExpected() {
    when(execution.run(any())).thenReturn(new RunResult(Status.OK, "stdout", "stderr", 1, 2));
    when(harness.match(any(), any(), anyString(), anyString()))
        .thenReturn(new Match(false, "a".repeat(5000), "debug"));
    var result = evaluator.evaluate(request(Model.FUNCTION_WRAPPER, true, true));
    assertThat(result.verdict()).isEqualTo(Verdict.WA);
    assertThat(result.results().getFirst().actualOutputSnippet()).hasSize(4096);
    assertThat(result.results().getFirst().userConsole()).isEqualTo("debug");
    assertThat(result.results().getFirst().stderrSnippet()).isNull();
  }

  @Test
  void standardIoRuntimeStderrRequiresSampleFlagAndPermission() {
    when(execution.run(any()))
        .thenReturn(new RunResult(Status.RE, "stdout", "s".repeat(5000), 1, 2));
    assertThat(
            evaluator
                .evaluate(request(Model.STANDARD_IO, true, true))
                .results()
                .getFirst()
                .stderrSnippet())
        .hasSize(4096);
    assertThat(
            evaluator
                .evaluate(request(Model.STANDARD_IO, true, false))
                .results()
                .getFirst()
                .stderrSnippet())
        .isNull();
    assertThat(
            evaluator
                .evaluate(request(Model.STANDARD_IO, false, true))
                .results()
                .getFirst()
                .stderrSnippet())
        .isNull();
    when(execution.run(any())).thenReturn(new RunResult(Status.OK, "stdout", "stderr", 1, 2));
    assertThat(
            evaluator
                .evaluate(request(Model.STANDARD_IO, true, true))
                .results()
                .getFirst()
                .stderrSnippet())
        .isNull();
  }

  @Test
  void inputFailureBecomesSpecFailureAndGuardHasFixedCompileMessage() {
    when(execution.run(any()))
        .thenReturn(new RunResult(Status.INPUT_ERROR, "secret", "secret", 1, 2));
    var invalid = evaluator.evaluate(request(Model.FUNCTION_WRAPPER, true, true));
    assertThat(invalid.failureReason()).isEqualTo("SPEC_UNAVAILABLE");
    assertThat(invalid.results()).isEmpty();
    when(execution.run(any()))
        .thenReturn(new RunResult(Status.SIGNATURE_ERROR, "secret", "secret", 1, 2));
    var signature = evaluator.evaluate(request(Model.FUNCTION_WRAPPER, true, true));
    assertThat(signature.verdict()).isEqualTo(Verdict.COMPILE_ERROR);
    assertThat(signature.compileMessage()).contains("solve(value)").doesNotContain("secret");
    assertThat(signature.results()).isEmpty();
  }

  @Test
  void mapsCompilerOutputAndHidesInternalCompilerFailure() {
    when(execution.compilation()).thenReturn(new Compilation(false, "raw secret"));
    when(harness.mapCompile(any(), anyString()))
        .thenReturn(new CompileReport(false, "Dòng 1: syntax error"));
    assertThat(evaluator.evaluate(request(Model.STANDARD_IO, true, true)).compileMessage())
        .isEqualTo("Dòng 1: syntax error");
    when(harness.mapCompile(any(), anyString())).thenReturn(new CompileReport(true, null));
    var internal = evaluator.evaluate(request(Model.STANDARD_IO, true, true));
    assertThat(internal.failureReason()).isEqualTo("SANDBOX_FAULT");
    assertThat(internal.compileMessage()).isNull();
  }

  @Test
  void infrastructureAndExpectedParseFailuresHaveDistinctSystemReasons() {
    when(execution.run(any())).thenThrow(new JudgeInfrastructureException());
    assertThat(evaluator.evaluate(request(Model.STANDARD_IO, true, true)).failureReason())
        .isEqualTo("SANDBOX_FAULT");
    doReturn(new RunResult(Status.OK, "stdout", "", 1, 2)).when(execution).run(any());
    when(harness.match(any(), any(), anyString(), anyString()))
        .thenThrow(new DomainRuleViolationException("HRN-050", "secret"));
    assertThat(evaluator.evaluate(request(Model.STANDARD_IO, true, true)).failureReason())
        .isEqualTo("SPEC_UNAVAILABLE");
    doThrow(new DomainRuleViolationException("OTHER", "failure"))
        .when(harness)
        .match(any(), any(), anyString(), anyString());
    assertThatThrownBy(() -> evaluator.evaluate(request(Model.STANDARD_IO, true, true)))
        .isInstanceOf(DomainRuleViolationException.class);
  }

  @Test
  void validatesLimitsAndDefensivelyCopiesCases() {
    assertThatThrownBy(() -> new Limits(0, 1, 1, 1)).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new Limits(300001, 1, 1, 1))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new Limits(1, 0, 1, 1)).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new Limits(1, 1, 0, 1)).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new Limits(1, 1, Integer.MAX_VALUE, 1))
        .isInstanceOf(IllegalArgumentException.class);
    assertThat(new Limits(1, 1, 1, 0).processes()).isZero();
    assertThatThrownBy(() -> new Limits(1, 1, 1, -1)).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new Limits(1, 1, 1, 61)).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> request(Model.STANDARD_IO, true, true).testcases().clear())
        .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  void oversizedWrapperDebugIsReBeforeMatchingAndIsPrivateForSampleAndHidden() {
    when(execution.run(any()))
        .thenReturn(
            new RunResult(Status.OK, "x".repeat(4097) + "\n@@APH-RESULT@@\n7\n", "secret", 1, 2));
    for (boolean sample : List.of(true, false)) {
      var result = evaluator.evaluate(request(Model.FUNCTION_WRAPPER, sample, true));
      assertThat(result.verdict()).isEqualTo(Verdict.RE);
      assertThat(result.results())
          .allSatisfy(
              row -> {
                assertThat(row.actualOutputSnippet()).isNull();
                assertThat(row.userConsole()).isNull();
                assertThat(row.stderrSnippet()).isNull();
              });
    }
    verify(harness, never()).match(any(), any(), anyString(), anyString());
  }

  @Test
  void debugBudgetDoesNotChangeEnginePriorityStandardIoOrMissingMarkerHandling() {
    String stdout = "x".repeat(4097) + "\n@@APH-RESULT@@\n7\n";
    for (Status status : List.of(Status.TLE, Status.MLE)) {
      when(execution.run(any())).thenReturn(new RunResult(status, stdout, "", 1, 2));
      assertThat(evaluator.evaluate(request(Model.FUNCTION_WRAPPER, true, true)).verdict().name())
          .isEqualTo(status.name());
    }
    when(execution.run(any())).thenReturn(new RunResult(Status.OK, stdout, "", 1, 2));
    assertThat(evaluator.evaluate(request(Model.STANDARD_IO, true, true)).verdict())
        .isEqualTo(Verdict.AC);
    when(execution.run(any())).thenReturn(new RunResult(Status.OK, "x".repeat(4097), "", 1, 2));
    when(harness.match(any(), any(), anyString(), anyString()))
        .thenReturn(new Match(false, "", ""));
    assertThat(evaluator.evaluate(request(Model.FUNCTION_WRAPPER, true, true)).verdict())
        .isEqualTo(Verdict.WA);
  }
}
