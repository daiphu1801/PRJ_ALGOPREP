package com.algoprep.judge.application.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.algoprep.common.exception.DomainRuleViolationException;
import com.algoprep.common.functionspec.Language;
import com.algoprep.common.functionspec.MatchingStrategy;
import com.algoprep.judge.application.dto.ExecutionModels.Compilation;
import com.algoprep.judge.application.dto.ExecutionModels.CompileReport;
import com.algoprep.judge.application.dto.ExecutionModels.Match;
import com.algoprep.judge.application.dto.ExecutionModels.Matching;
import com.algoprep.judge.application.dto.ExecutionModels.Model;
import com.algoprep.judge.application.dto.ExecutionModels.Program;
import com.algoprep.judge.application.dto.ExecutionModels.RunResult;
import com.algoprep.judge.application.dto.ExecutionModels.Status;
import com.algoprep.judge.application.dto.GradingSpecification;
import com.algoprep.judge.application.dto.GradingTestcase;
import com.algoprep.judge.application.dto.GradingTestcaseSnapshot;
import com.algoprep.judge.application.dto.WorkerPreparation.Prepared;
import com.algoprep.judge.application.ports.out.HarnessEvaluationPort;
import com.algoprep.judge.application.ports.out.JudgeExecutionPort;
import com.algoprep.judge.application.ports.out.JudgeExecutionPort.CompiledExecution;
import com.algoprep.judge.application.ports.out.ProblemGradingSpecPort;
import com.algoprep.judge.application.ports.out.WorkerProgramPort;
import com.algoprep.judge.domain.exception.JudgeInfrastructureException;
import com.algoprep.judge.domain.exception.WorkerPersistenceException;
import com.algoprep.judge.domain.model.WorkerJob;
import com.algoprep.judge.domain.model.WorkerRecords.FinishCommand;
import com.algoprep.judge.domain.model.WorkerRecords.FinishKind;
import com.algoprep.judge.domain.model.WorkerRecords.TestcaseWrite;
import com.algoprep.judge.domain.model.WorkerRecords.Token;
import com.algoprep.judge.domain.model.WorkerSettings;
import com.algoprep.judge.domain.ports.out.WorkerJobRepository;
import com.algoprep.judge.domain.ports.out.WorkerLeasePort;
import com.algoprep.judge.domain.ports.out.WorkerLeasePort.Lease;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class WorkerRuntimeUnitTest {
  static class Fixture {
    final UUID id = UUID.randomUUID();
    final UUID problem = UUID.randomUUID();
    final WorkerJobRepository jobs = mock(WorkerJobRepository.class);
    final WorkerLeasePort leases = mock(WorkerLeasePort.class);
    final Lease lease = mock(Lease.class);
    final ProblemGradingSpecPort problems = mock(ProblemGradingSpecPort.class);
    final WorkerProgramPort programs = mock(WorkerProgramPort.class);
    final JudgeExecutionPort engine = mock(JudgeExecutionPort.class);
    final CompiledExecution compiled = mock(CompiledExecution.class);
    final HarnessEvaluationPort harness = mock(HarnessEvaluationPort.class);
    final WorkerPersistence persistence = mock(WorkerPersistence.class);
    final List<TestcaseWrite> writes = new ArrayList<>();
    final List<FinishCommand> finishes = new ArrayList<>();
    String status = "PENDING";
    boolean partial;
    final WorkerSettings settings =
        new WorkerSettings(BigDecimal.ONE, BigDecimal.ONE, 1000, 256, 64, 10, false, true, true);
    final WorkerProgramLoader loader = new WorkerProgramLoader(problems, programs);
    final ProcessQueuedSubmissionCommandHandler handler;

    Fixture() {
      when(leases.acquire(anyString())).thenReturn(Optional.of(lease));
      when(jobs.find(id)).thenAnswer(call -> Optional.of(job()));
      when(jobs.settings("PYTHON")).thenReturn(settings);
      when(problems.loadGradingSpec(problem)).thenReturn(Optional.of(spec(9)));
      when(problems.loadTestcaseSnapshot(problem, 7)).thenReturn(snapshot());
      when(programs.prepare(any())).thenReturn(prepared(Model.STANDARD_IO));
      when(engine.compile(any())).thenReturn(compiled);
      when(compiled.compilation()).thenReturn(new Compilation(true, ""));
      when(compiled.run(any())).thenReturn(new RunResult(Status.OK, "1\n", "", 2, 3));
      when(harness.match(any(), any(), anyString(), anyString()))
          .thenAnswer(
              c -> new Match(c.getArgument(2).equals(c.getArgument(3)), c.getArgument(2), ""));
      wirePersistence();
      var execution =
          new WorkerExecution(
              engine, harness, persistence, new WorkerCaseEvaluation(harness), 1073741824);
      var attempt = new WorkerAttemptProcessor(loader, problems, persistence, execution);
      handler = new ProcessQueuedSubmissionCommandHandler(jobs, leases, attempt, persistence);
    }

    private void wirePersistence() {
      when(persistence.compiling(any(), org.mockito.ArgumentMatchers.anyInt(), any()))
          .thenAnswer(
              c -> {
                status = "COMPILING";
                return true;
              });
      when(persistence.judging(any(), org.mockito.ArgumentMatchers.anyInt(), any()))
          .thenAnswer(
              c -> {
                status = "JUDGING";
                return true;
              });
      when(persistence.token(any(), any(), any()))
          .thenAnswer(c -> Optional.of(new Token(UUID.randomUUID(), UUID.randomUUID())));
      when(persistence.record(any(), any()))
          .thenAnswer(
              c -> {
                writes.add(c.getArgument(0));
                return true;
              });
      org.mockito.Mockito.doAnswer(
              c -> {
                FinishKind kind = c.getArgument(1);
                String detail = c.getArgument(2);
                finishes.add(
                    new FinishCommand(
                        id,
                        kind,
                        kind == FinishKind.SYSTEM_ERROR ? detail : null,
                        kind == FinishKind.COMPILE_ERROR ? detail : null));
                status = kind == FinishKind.COMPLETE ? "ACCEPTED" : kind.name();
                return null;
              })
          .when(persistence)
          .finish(any(), any(), org.mockito.ArgumentMatchers.nullable(String.class), any());
    }

    WorkerJob job() {
      return new WorkerJob(
          id, problem, "PYTHON", "STANDARD_IO", "private source", 7, status, partial);
    }

    GradingSpecification spec(int version) {
      return new GradingSpecification(
          new GradingSpecification.Problem(problem, true, true, true),
          new GradingSpecification.Limits(1000, 256, 64, null),
          new GradingSpecification.Versions(version, 11),
          new GradingSpecification.Matching("EXACT", null),
          List.of());
    }

    GradingTestcaseSnapshot snapshot() {
      return new GradingTestcaseSnapshot(
          problem, 7, List.of(testcase(2, "HIDDEN"), testcase(1, "SAMPLE")));
    }

    GradingTestcase testcase(int position, String visibility) {
      return new GradingTestcase(
          UUID.randomUUID(),
          visibility,
          position,
          (position + "\n").getBytes(StandardCharsets.UTF_8),
          (position + "\n").getBytes(StandardCharsets.UTF_8));
    }

    Prepared prepared(Model model) {
      return new Prepared(
          new Program(Language.PYTHON, model, Map.of(), null, null, "echo(int)", "@@R@@"),
          new Matching(MatchingStrategy.EXACT, null, null));
    }
  }

  @Test
  void pinnedSnapshotIsSortedAndEveryCaseRunsWithOneCompilation() {
    var f = new Fixture();
    f.handler.process(f.id);
    verify(f.engine, times(1)).compile(any());
    verify(f.compiled, times(2)).run(any());
    verify(f.compiled).close();
    verify(f.lease).close();
    assertThat(f.writes).hasSize(2);
    assertThat(f.writes.stream().map(TestcaseWrite::verdict).toList()).containsExactly("AC", "WA");
    assertThat(f.writes.get(0).sample()).isTrue();
    assertThat(f.writes.get(1).actualOutput()).isNull();
    assertThat(f.finishes.getLast().kind()).isEqualTo(FinishKind.COMPLETE);
    verify(f.jobs, times(1)).settings("PYTHON");
    verify(f.problems).loadTestcaseSnapshot(f.problem, 7);
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "ACCEPTED",
        "WRONG_ANSWER",
        "TIME_LIMIT_EXCEEDED",
        "RUNTIME_ERROR",
        "COMPILE_ERROR",
        "SYSTEM_ERROR"
      })
  void terminalDeliveryDoesNotReadConfigOrExecuteAgain(String status) {
    var f = new Fixture();
    f.status = status;
    f.handler.process(f.id);
    verify(f.jobs, never()).settings(anyString());
    verify(f.engine, never()).compile(any());
    assertThat(f.finishes).isEmpty();
  }

  @Test
  void busyMissingAndPartiallyGradedJobsTakeTheirExplicitSkipOrTimeoutBranches() {
    var busy = new Fixture();
    when(busy.leases.acquire(anyString())).thenReturn(Optional.empty());
    busy.handler.process(busy.id);
    verify(busy.jobs, never()).find(any());
    var missing = new Fixture();
    when(missing.jobs.find(any())).thenReturn(Optional.empty());
    missing.handler.process(missing.id);
    verify(missing.engine, never()).compile(any());
    var partial = new Fixture();
    partial.partial = true;
    partial.handler.process(partial.id);
    assertThat(partial.finishes.getLast().failureReason()).isEqualTo("WORKER_TIMEOUT");
    verify(partial.engine, never()).compile(any());
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void compilerUserErrorsAndHarnessErrorsHaveDifferentFinalOutcomes(boolean internal) {
    var f = new Fixture();
    when(f.compiled.compilation()).thenReturn(new Compilation(false, "private compiler output"));
    when(f.harness.mapCompile(any(), anyString()))
        .thenReturn(new CompileReport(internal, internal ? null : "mapped user error"));
    f.handler.process(f.id);
    assertThat(f.finishes.getLast().kind())
        .isEqualTo(internal ? FinishKind.SYSTEM_ERROR : FinishKind.COMPILE_ERROR);
    verify(f.compiled, never()).run(any());
    assertThat(f.finishes.toString()).doesNotContain("private");
  }

  @ParameterizedTest
  @CsvSource({"TLE,TLE", "MLE,MLE", "RE,RE"})
  void runtimeFailuresStillRunEveryRemainingTestcase(String status, String verdict) {
    var f = new Fixture();
    when(f.compiled.run(any()))
        .thenReturn(new RunResult(Status.valueOf(status), "", "private", 5, 6));
    f.handler.process(f.id);
    assertThat(f.writes).hasSize(2);
    assertThat(f.writes.getFirst().verdict()).isEqualTo(verdict);
  }

  @ParameterizedTest
  @CsvSource({
    "HRN-001,SPEC_UNAVAILABLE",
    "HRN-002,SPEC_UNAVAILABLE",
    "HRN-003,SANDBOX_FAULT",
    "HRN-004,SPEC_UNAVAILABLE",
    "HRN-005,SPEC_UNAVAILABLE",
    "HRN-006,SPEC_UNAVAILABLE",
    "HRN-030,SANDBOX_FAULT",
    "HRN-031,SANDBOX_FAULT"
  })
  void preparationFailuresBecomeSystemErrorsWithTheCorrectReason(String code, String reason) {
    var f = new Fixture();
    when(f.programs.prepare(any())).thenThrow(new DomainRuleViolationException(code, "private"));
    f.handler.process(f.id);
    assertThat(f.finishes.getLast().failureReason()).isEqualTo(reason);
    verify(f.engine, never()).compile(any());
  }

  @Test
  void changedSpecIsReadAgainOnceBeforeTheOneActualCompilation() {
    var f = new Fixture();
    when(f.problems.loadGradingSpec(f.problem))
        .thenReturn(Optional.of(f.spec(9)), Optional.of(f.spec(10)));
    when(f.programs.prepare(any()))
        .thenThrow(new DomainRuleViolationException("HRN-002", "changed"))
        .thenReturn(f.prepared(Model.STANDARD_IO));
    f.handler.process(f.id);
    verify(f.problems, times(2)).loadGradingSpec(f.problem);
    verify(f.engine, times(1)).compile(any());
    verify(f.persistence).compiling(f.id, 10, f.lease);
  }

  @ParameterizedTest
  @ValueSource(strings = {"INPUT_ERROR", "SIGNATURE_ERROR"})
  void harnessRuntimeSignalsNeverPersistRawErrorText(String signal) {
    var f = new Fixture();
    when(f.compiled.run(any()))
        .thenReturn(new RunResult(Status.valueOf(signal), "private", "private traceback", 1, 1));
    f.handler.process(f.id);
    assertThat(f.writes).isEmpty();
    assertThat(f.finishes.getLast().kind())
        .isEqualTo(
            signal.equals("SIGNATURE_ERROR") ? FinishKind.COMPILE_ERROR : FinishKind.SYSTEM_ERROR);
    assertThat(f.finishes.toString()).doesNotContain("private");
  }

  @Test
  void missingCorruptAndMismatchedPinnedSnapshotsAreNotReplacedWithLiveCases() {
    var f = new Fixture();
    when(f.problems.loadTestcaseSnapshot(f.problem, 7))
        .thenReturn(new GradingTestcaseSnapshot(f.problem, 8, List.of(f.testcase(1, "SAMPLE"))));
    f.handler.process(f.id);
    assertThat(f.finishes.getLast().failureReason()).isEqualTo("SPEC_UNAVAILABLE");
    verify(f.engine, never()).compile(any());
    var absent = new Fixture();
    when(absent.problems.loadGradingSpec(absent.problem)).thenReturn(Optional.empty());
    absent.handler.process(absent.id);
    assertThat(absent.finishes.getLast().failureReason()).isEqualTo("SPEC_UNAVAILABLE");
  }

  @Test
  void invalidUtf8AndInvalidExpectedOutputAreSpecFailures() {
    var f = new Fixture();
    when(f.problems.loadTestcaseSnapshot(f.problem, 7))
        .thenReturn(
            new GradingTestcaseSnapshot(
                f.problem,
                7,
                List.of(
                    new GradingTestcase(
                        UUID.randomUUID(), "SAMPLE", 1, new byte[] {(byte) 255}, new byte[] {1}))));
    f.handler.process(f.id);
    assertThat(f.finishes.getLast().failureReason()).isEqualTo("SPEC_UNAVAILABLE");
    verify(f.compiled, never()).run(any());
    var expected = new Fixture();
    when(expected.harness.match(any(), any(), anyString(), anyString()))
        .thenThrow(new DomainRuleViolationException("HRN-050", "private expected"));
    expected.handler.process(expected.id);
    assertThat(expected.finishes.getLast().failureReason()).isEqualTo("SPEC_UNAVAILABLE");
  }

  @Test
  void databaseOrOwnershipFailurePropagatesWithoutInventingAFinalState() {
    var f = new Fixture();
    when(f.persistence.record(any(), any())).thenThrow(new WorkerPersistenceException());
    assertThatThrownBy(() -> f.handler.process(f.id))
        .isInstanceOf(WorkerPersistenceException.class);
    assertThat(f.finishes).isEmpty();
    verify(f.lease).close();
    var engine = new Fixture();
    when(engine.engine.compile(any())).thenThrow(new JudgeInfrastructureException());
    engine.handler.process(engine.id);
    assertThat(engine.finishes.getLast().failureReason()).isEqualTo("SANDBOX_FAULT");
  }

  @Test
  void unsupportedNetworkConfigurationFailsClosedAndLimitsUseHalfUpRounding() {
    var f = new Fixture();
    when(f.jobs.settings("PYTHON"))
        .thenReturn(
            new WorkerSettings(
                BigDecimal.ONE, BigDecimal.ONE, 1000, 256, 64, 10, true, true, true));
    f.handler.process(f.id);
    verify(f.engine, never()).compile(any());
    assertThat(f.finishes.getLast().failureReason()).isEqualTo("SANDBOX_FAULT");
    var settings =
        new WorkerSettings(
            new BigDecimal("1.5"), new BigDecimal("1.5"), 1001, 65, 64, 10, false, false, true);
    var spec =
        new GradingSpecification(
            f.spec(9).problem(),
            new GradingSpecification.Limits(0, 0, 0, null),
            f.spec(9).versions(),
            f.spec(9).matching(),
            List.of());
    var limits = f.loader.runLimits(spec, settings);
    assertThat(limits.timeMs()).isEqualTo(1502);
    assertThat(limits.memoryBytes()).isEqualTo(98 * 1048576L);
    assertThat(limits.outputBytes()).isEqualTo(65536);
    assertThat(limits.processes()).isZero();
  }
}
