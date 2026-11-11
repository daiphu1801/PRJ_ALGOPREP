package com.algoprep.bootstrap.problembank;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.algoprep.bootstrap.judge.HarnessProgramBridge;
import com.algoprep.common.functionspec.Language;
import com.algoprep.harness.application.dto.PackagedProgram;
import com.algoprep.harness.application.prepare.ProgramPackaging;
import com.algoprep.judge.application.dto.ExecutionModels.Compilation;
import com.algoprep.judge.application.dto.ExecutionModels.CompileRequest;
import com.algoprep.judge.application.dto.ExecutionModels.Match;
import com.algoprep.judge.application.dto.ExecutionModels.Matching;
import com.algoprep.judge.application.dto.ExecutionModels.Program;
import com.algoprep.judge.application.dto.ExecutionModels.RunRequest;
import com.algoprep.judge.application.dto.ExecutionModels.RunResult;
import com.algoprep.judge.application.dto.ExecutionModels.Status;
import com.algoprep.judge.application.dto.WorkerPreparation.Prepared;
import com.algoprep.judge.application.ports.in.GetGeneratorSandboxLimitsUseCase;
import com.algoprep.judge.application.ports.in.GetLanguageMultipliersUseCase;
import com.algoprep.judge.application.ports.out.JudgeExecutionPort;
import com.algoprep.judge.application.ports.out.JudgeExecutionPort.CompiledExecution;
import com.algoprep.judge.application.ports.out.SampleProgramPort;
import com.algoprep.judge.domain.model.GeneratorSandboxLimits;
import com.algoprep.judge.domain.model.LanguageMultipliers;
import com.algoprep.problembank.application.dto.ReferenceSolutionRun;
import com.algoprep.problembank.application.dto.TestcaseGeneration.ReferenceStatus;
import com.algoprep.problembank.application.dto.TestcaseGeneration.ScriptStatus;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/** The mapping the sandbox adapter owns; the harness and the sandbox behind it have their tests. */
class GeneratorSandboxBridgeTest {
  private final ProgramPackaging packaging = mock(ProgramPackaging.class);
  private final HarnessProgramBridge harness = mock(HarnessProgramBridge.class);
  private final JudgeExecutionPort engine = mock(JudgeExecutionPort.class);
  private final GetGeneratorSandboxLimitsUseCase limits =
      mock(GetGeneratorSandboxLimitsUseCase.class);
  private final SampleProgramPort programs = mock(SampleProgramPort.class);
  private final GetLanguageMultipliersUseCase multipliers =
      mock(GetLanguageMultipliersUseCase.class);
  private final CompiledExecution execution = mock(CompiledExecution.class);
  private final Program program = mock(Program.class);
  private GeneratorSandboxBridge bridge;

  @BeforeEach
  void setup() {
    when(packaging.standardIo(eq(Language.PYTHON), anyString()))
        .thenReturn(mock(PackagedProgram.class));
    when(harness.program(any())).thenReturn(program);
    when(engine.compile(any())).thenReturn(execution);
    when(execution.compilation()).thenReturn(new Compilation(true, ""));
    when(limits.getGeneratorSandboxLimits()).thenReturn(new GeneratorSandboxLimits(7, 2));
    bridge =
        new GeneratorSandboxBridge(
            new GeneratorScriptRunner(packaging, harness, engine, limits),
            programs,
            engine,
            new GeneratorSandboxBridge.ReferenceConversions(harness, multipliers));
  }

  private static RunResult run(Status status, String stdout) {
    return new RunResult(status, stdout, "", 5, 100);
  }

  @Test
  void theGeneratorGetsTheSeedFromTheEnvironmentAndTheConfiguredLimits() {
    when(execution.run(any())).thenReturn(run(Status.OK, "###CASE TYPICAL\n1\n"));

    var out = bridge.runGenerator("print(1)", 42L);

    assertThat(out.status()).isEqualTo(ScriptStatus.OK);
    assertThat(out.stdout()).isEqualTo("###CASE TYPICAL\n1\n");
    var source = ArgumentCaptor.forClass(String.class);
    verify(packaging).standardIo(eq(Language.PYTHON), source.capture());
    assertThat(source.getValue()).startsWith("import os\nos.environ[\"SEED\"] = \"42\"\n");
    assertThat(source.getValue()).endsWith("print(1)");
    var request = ArgumentCaptor.forClass(RunRequest.class);
    verify(execution).run(request.capture());
    assertThat(request.getValue().limits().timeMs()).isEqualTo(7000);
    assertThat(request.getValue().limits().outputBytes()).isEqualTo(2L * 1_048_576);
    assertThat(request.getValue().stdin()).isEmpty();
  }

  @Test
  void theFallbackLimitsApplyWhenTheJudgeCannotAnswer() {
    when(limits.getGeneratorSandboxLimits()).thenThrow(new IllegalStateException("down"));
    when(execution.run(any())).thenReturn(run(Status.OK, "x"));

    bridge.runGenerator("s", 1L);

    var request = ArgumentCaptor.forClass(RunRequest.class);
    verify(execution).run(request.capture());
    assertThat(request.getValue().limits().timeMs()).isEqualTo(30_000);
    assertThat(request.getValue().limits().outputBytes()).isEqualTo(100L * 1_048_576);
  }

  @Test
  void everySandboxStatusMapsToTheDdReason() {
    when(execution.run(any()))
        .thenReturn(
            run(Status.TLE, ""),
            run(Status.RE, "y".repeat(2 * 1_048_576)),
            run(Status.RE, "short"),
            run(Status.MLE, ""));

    assertThat(bridge.runGenerator("s", 1L).status()).isEqualTo(ScriptStatus.TIMEOUT);
    assertThat(bridge.runGenerator("s", 1L).status()).isEqualTo(ScriptStatus.OUTPUT_TOO_LARGE);
    assertThat(bridge.runGenerator("s", 1L).status()).isEqualTo(ScriptStatus.SCRIPT_ERROR);
    assertThat(bridge.runGenerator("s", 1L).status()).isEqualTo(ScriptStatus.SCRIPT_ERROR);
  }

  @Test
  void aScriptThatDoesNotCompileIsAScriptError() {
    when(execution.compilation()).thenReturn(new Compilation(false, "SyntaxError"));

    assertThat(bridge.runGenerator("s", 1L).status()).isEqualTo(ScriptStatus.SCRIPT_ERROR);
    assertThat(bridge.validate("v", List.of("1", "2"))).containsExactly(false, false);
  }

  @Test
  void theValidatorPassesOnlyOnExitZero() {
    when(execution.run(any()))
        .thenReturn(run(Status.OK, ""), run(Status.RE, ""), run(Status.TLE, ""));

    assertThat(bridge.validate("v", List.of("a", "b", "c"))).containsExactly(true, false, false);
  }

  private ReferenceSolutionRun.Request referenceRequest() {
    return new ReferenceSolutionRun.Request(
        UUID.randomUUID(),
        "PYTHON",
        false,
        "print(1)",
        3,
        new ReferenceSolutionRun.Limits(1000, 256, 64, 10),
        List.of(
            new ReferenceSolutionRun.Case(UUID.randomUUID(), "1", ""),
            new ReferenceSolutionRun.Case(UUID.randomUUID(), "2", ""),
            new ReferenceSolutionRun.Case(UUID.randomUUID(), "3", "")));
  }

  @Test
  void theSampleSolutionOutputIsReadBackCaseByCase() {
    when(multipliers.getLanguageMultipliers("PYTHON"))
        .thenReturn(new LanguageMultipliers(new BigDecimal("3"), new BigDecimal("1.5")));
    var matching = mock(Matching.class);
    when(programs.prepare(any())).thenReturn(new Prepared(program, matching));
    when(execution.run(any()))
        .thenReturn(run(Status.OK, "raw"), run(Status.TLE, ""), run(Status.RE, ""));
    when(harness.match(program, matching, "raw", "")).thenReturn(new Match(false, "10", ""));

    var out = bridge.runReference(referenceRequest());

    assertThat(out)
        .extracting("status")
        .containsExactly(ReferenceStatus.OK, ReferenceStatus.TIMEOUT, ReferenceStatus.FAILED);
    assertThat(out.get(0).output()).isEqualTo("10");
    var request = ArgumentCaptor.forClass(RunRequest.class);
    verify(execution, org.mockito.Mockito.times(3)).run(request.capture());
    assertThat(request.getAllValues().get(0).limits().timeMs()).isEqualTo(3000);
    assertThat(request.getAllValues().get(0).stdin()).isEqualTo("1");
    var compile = ArgumentCaptor.forClass(CompileRequest.class);
    verify(engine).compile(compile.capture());
    assertThat(compile.getValue().limits().timeMs()).isEqualTo(10_000);
  }

  @Test
  void anOutputThatTheHarnessCannotExtractIsAFailedCase() {
    when(multipliers.getLanguageMultipliers("PYTHON"))
        .thenReturn(new LanguageMultipliers(BigDecimal.ONE, BigDecimal.ONE));
    var matching = mock(Matching.class);
    when(programs.prepare(any())).thenReturn(new Prepared(program, matching));
    when(execution.run(any())).thenReturn(run(Status.OK, "raw"));
    when(harness.match(any(), any(), anyString(), anyString()))
        .thenReturn(new Match(false, null, ""));

    var out = bridge.runReference(referenceRequest());

    assertThat(out.get(0).status()).isEqualTo(ReferenceStatus.FAILED);
  }

  @Test
  void aSampleSolutionThatNoLongerCompilesIsASandboxFault() {
    when(multipliers.getLanguageMultipliers("PYTHON"))
        .thenReturn(new LanguageMultipliers(BigDecimal.ONE, BigDecimal.ONE));
    when(programs.prepare(any())).thenReturn(new Prepared(program, mock(Matching.class)));
    when(execution.compilation()).thenReturn(new Compilation(false, "boom"));

    assertThatThrownBy(() -> bridge.runReference(referenceRequest()))
        .isInstanceOf(IllegalStateException.class);
  }
}
