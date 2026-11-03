package com.algoprep.judge.application.command;

import com.algoprep.judge.application.dto.ExecutionModels.Model;
import com.algoprep.judge.application.dto.ExecutionModels.RunResult;
import com.algoprep.judge.application.dto.ExecutionModels.Status;
import com.algoprep.judge.application.dto.GradingTestcase;
import com.algoprep.judge.application.ports.out.HarnessEvaluationPort;
import com.algoprep.judge.domain.model.WorkerRecords.TestcaseWrite;
import com.algoprep.judge.domain.model.WorkerRecords.Token;
import com.algoprep.judge.domain.model.WrapperDebugBudget;
import org.springframework.stereotype.Component;

@Component
class WorkerCaseEvaluation {
  private final HarnessEvaluationPort harness;

  WorkerCaseEvaluation(HarnessEvaluationPort harness) {
    this.harness = harness;
  }

  TestcaseWrite evaluate(
      WorkerExecutionRequest request,
      GradingTestcase testcase,
      RunResult run,
      Token token,
      int order) {
    var prepared = request.prepared();
    var program = prepared.program();
    boolean sample = testcase.visibility().equals("SAMPLE");
    boolean debugExceeded =
        run.status() == Status.OK
            && program.model() == Model.FUNCTION_WRAPPER
            && WrapperDebugBudget.exceeded(run.stdout(), program.resultSentinel());
    String verdict =
        switch (run.status()) {
          case TLE -> "TLE";
          case MLE -> "MLE";
          case RE -> "RE";
          default -> debugExceeded ? "RE" : "AC";
        };
    String actual = null;
    if (run.status() == Status.OK && !debugExceeded) {
      var match =
          harness.match(
              program,
              prepared.matching(),
              run.stdout(),
              WorkerExecution.decode(testcase.expectedOutput()));
      verdict = match.matches() ? "AC" : "WA";
      actual = sample ? match.actualOutput() : null;
    }
    String stderr =
        request.settings().returnStderr()
                && sample
                && !verdict.equals("AC")
                && program.model() == Model.STANDARD_IO
            ? run.stderr()
            : null;
    return new TestcaseWrite(
        request.job().id(),
        testcase.testcaseId(),
        token.id(),
        order,
        sample,
        verdict,
        Math.toIntExact(run.runtimeMs()),
        Math.toIntExact(run.memoryKb()),
        actual,
        stderr,
        request.settings().returnStderr());
  }
}
