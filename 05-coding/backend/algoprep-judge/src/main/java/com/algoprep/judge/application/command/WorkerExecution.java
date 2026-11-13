package com.algoprep.judge.application.command;

import com.algoprep.judge.application.dto.ExecutionModels.CompileRequest;
import com.algoprep.judge.application.dto.ExecutionModels.Limits;
import com.algoprep.judge.application.dto.ExecutionModels.RunRequest;
import com.algoprep.judge.application.dto.ExecutionModels.Status;
import com.algoprep.judge.application.ports.out.HarnessEvaluationPort;
import com.algoprep.judge.application.ports.out.JudgeExecutionPort;
import com.algoprep.judge.application.ports.out.JudgeExecutionPort.CompiledExecution;
import com.algoprep.judge.domain.exception.ProblemGradingReadException;
import com.algoprep.judge.domain.model.WorkerRecords.FinishKind;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.StandardCharsets;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
class WorkerExecution {
  private final JudgeExecutionPort engine;
  private final HarnessEvaluationPort harness;
  private final WorkerPersistence persistence;
  private final WorkerCaseEvaluation evaluation;
  private final long compileMemory;

  WorkerExecution(
      JudgeExecutionPort engine,
      HarnessEvaluationPort harness,
      WorkerPersistence persistence,
      WorkerCaseEvaluation evaluation,
      @Value("${judge.worker.compile-memory-bytes:1073741824}") long compileMemory) {
    this.engine = engine;
    this.harness = harness;
    this.persistence = persistence;
    this.evaluation = evaluation;
    this.compileMemory = compileMemory;
  }

  void run(WorkerExecutionRequest request) {
    var limits =
        new Limits(
            Math.multiplyExact(request.settings().compileSeconds(), 1000L),
            compileMemory,
            65536,
            request.limits().processes());
    request.lease().check();
    try (var execution = engine.compile(new CompileRequest(request.prepared().program(), limits))) {
      request.lease().check();
      if (!execution.compilation().success()) {
        compileError(request, execution);
        return;
      }
      if (!persistence.judging(
          request.job().id(), request.snapshot().testcases().size(), request.lease())) {
        return;
      }
      if (!runCases(request, execution)) {
        return;
      }
    }
    persistence.finish(request.job().id(), FinishKind.COMPLETE, null, request.lease());
  }

  private void compileError(WorkerExecutionRequest request, CompiledExecution execution) {
    var report =
        harness.mapCompile(request.prepared().program(), execution.compilation().compilerOutput());
    persistence.finish(
        request.job().id(),
        report.internal() ? FinishKind.SYSTEM_ERROR : FinishKind.COMPILE_ERROR,
        report.internal() ? "SANDBOX_FAULT" : report.message(),
        request.lease());
  }

  private boolean runCases(WorkerExecutionRequest request, CompiledExecution execution) {
    var testcases = request.snapshot().testcases();
    for (int i = 0; i < testcases.size(); i++) {
      var testcase = testcases.get(i);
      var token = persistence.token(request.job().id(), testcase.testcaseId(), request.lease());
      if (token.isEmpty()) {
        return false;
      }
      var run = execution.run(new RunRequest(decode(testcase.input()), request.limits()));
      request.lease().check();
      if (run.status() == Status.INPUT_ERROR) {
        throw new ProblemGradingReadException(ProblemGradingReadException.Reason.SPEC_UNAVAILABLE);
      }
      if (run.status() == Status.SIGNATURE_ERROR) {
        signatureError(request);
        return false;
      }
      if (!persistence.record(
          evaluation.evaluate(request, testcase, run, token.orElseThrow(), i + 1),
          request.lease())) {
        return false;
      }
    }
    return true;
  }

  private void signatureError(WorkerExecutionRequest request) {
    String message =
        "Không tìm thấy hàm đúng chữ ký trong mã của bạn. Hãy kiểm tra tên hàm, số tham số và kiểu: "
            + request.prepared().program().signatureText()
            + ".";
    persistence.finish(request.job().id(), FinishKind.COMPILE_ERROR, message, request.lease());
  }

  static String decode(byte[] bytes) {
    try {
      return StandardCharsets.UTF_8.newDecoder().decode(ByteBuffer.wrap(bytes)).toString();
    } catch (CharacterCodingException e) {
      throw new ProblemGradingReadException(ProblemGradingReadException.Reason.SPEC_UNAVAILABLE);
    }
  }
}
