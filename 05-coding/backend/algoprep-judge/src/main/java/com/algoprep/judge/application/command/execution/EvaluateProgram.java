package com.algoprep.judge.application.command;

import com.algoprep.common.exception.DomainRuleViolationException;
import com.algoprep.judge.application.dto.ExecutionModels.CompileRequest;
import com.algoprep.judge.application.dto.ExecutionModels.EvaluationRequest;
import com.algoprep.judge.application.dto.ExecutionModels.EvaluationResult;
import com.algoprep.judge.application.dto.ExecutionModels.Model;
import com.algoprep.judge.application.dto.ExecutionModels.RunRequest;
import com.algoprep.judge.application.dto.ExecutionModels.RunResult;
import com.algoprep.judge.application.dto.ExecutionModels.Status;
import com.algoprep.judge.application.dto.ExecutionModels.Testcase;
import com.algoprep.judge.application.dto.ExecutionModels.TestcaseResult;
import com.algoprep.judge.application.dto.ExecutionModels.Verdict;
import com.algoprep.judge.application.ports.in.EvaluateProgramUseCase;
import com.algoprep.judge.application.ports.out.HarnessEvaluationPort;
import com.algoprep.judge.application.ports.out.JudgeExecutionPort;
import com.algoprep.judge.domain.exception.JudgeInfrastructureException;
import com.algoprep.judge.domain.model.WrapperDebugBudget;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

/** Component execution only. Persisted submission orchestration remains deferred. */
@Service
final class EvaluateProgram implements EvaluateProgramUseCase {
  private final JudgeExecutionPort engine;
  private final HarnessEvaluationPort harness;

  EvaluateProgram(JudgeExecutionPort engine, HarnessEvaluationPort harness) {
    this.engine = engine;
    this.harness = harness;
  }

  @Override
  public EvaluationResult evaluate(EvaluationRequest request) {
    var results = new ArrayList<TestcaseResult>();
    try (var execution =
        engine.compile(new CompileRequest(request.program(), request.compileLimits()))) {
      if (!execution.compilation().success()) {
        var report =
            harness.mapCompile(request.program(), execution.compilation().compilerOutput());
        return report.internal()
            ? failure("SANDBOX_FAULT", results)
            : new EvaluationResult(Verdict.COMPILE_ERROR, null, report.message(), List.of());
      }
      for (var testcase : request.testcases()) {
        var run = execution.run(new RunRequest(testcase.stdin(), testcase.limits()));
        if (run.status() == Status.INPUT_ERROR) {
          return failure("SPEC_UNAVAILABLE", results);
        }
        if (run.status() == Status.SIGNATURE_ERROR) {
          return new EvaluationResult(
              Verdict.COMPILE_ERROR,
              null,
              "Không tìm thấy hàm đúng chữ ký trong mã của bạn. Hãy kiểm tra tên hàm, số tham số và kiểu: "
                  + request.program().signatureText()
                  + ".",
              List.of());
        }
        results.add(testcaseResult(request, testcase, run, results.size() + 1));
      }
      var verdict =
          results.stream()
              .filter(row -> row.verdict() != Verdict.AC)
              .map(TestcaseResult::verdict)
              .findFirst()
              .orElse(Verdict.AC);
      return new EvaluationResult(verdict, null, null, results);
    } catch (JudgeInfrastructureException unavailable) {
      return failure("SANDBOX_FAULT", results);
    } catch (DomainRuleViolationException invalid) {
      if (!"HRN-050".equals(invalid.errorCode())) {
        throw invalid;
      }
      return failure("SPEC_UNAVAILABLE", results);
    }
  }

  private TestcaseResult testcaseResult(
      EvaluationRequest request, Testcase testcase, RunResult run, int order) {
    boolean debugExceeded =
        run.status() == Status.OK
            && request.program().model() == Model.FUNCTION_WRAPPER
            && WrapperDebugBudget.exceeded(run.stdout(), request.program().resultSentinel());
    var verdict =
        switch (run.status()) {
          case TLE -> Verdict.TLE;
          case MLE -> Verdict.MLE;
          case RE -> Verdict.RE;
          default -> debugExceeded ? Verdict.RE : Verdict.AC;
        };
    String actual = null;
    String console = null;
    if (run.status() == Status.OK && !debugExceeded) {
      var match =
          harness.match(
              request.program(), request.matching(), run.stdout(), testcase.expectedOutput());
      verdict = match.matches() ? Verdict.AC : Verdict.WA;
      actual = testcase.sample() ? snippet(match.actualOutput()) : null;
      console = testcase.sample() ? snippet(match.userConsole()) : null;
    }
    String stderr =
        request.returnStderr()
                && testcase.sample()
                && verdict != Verdict.AC
                && request.program().model() == Model.STANDARD_IO
            ? snippet(run.stderr())
            : null;
    return new TestcaseResult(
        order, verdict, run.runtimeMs(), run.memoryKb(), actual, console, stderr);
  }

  private EvaluationResult failure(String reason, List<TestcaseResult> results) {
    return new EvaluationResult(Verdict.SYSTEM_ERROR, reason, null, results);
  }

  private String snippet(String text) {
    return text.substring(0, Math.min(4096, text.length()));
  }
}
