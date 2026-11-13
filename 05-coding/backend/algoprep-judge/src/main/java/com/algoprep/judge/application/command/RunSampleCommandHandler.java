package com.algoprep.judge.application.command;

import com.algoprep.judge.application.dto.ExecutionModels.EvaluationRequest;
import com.algoprep.judge.application.dto.ExecutionModels.Limits;
import com.algoprep.judge.application.dto.ExecutionModels.Testcase;
import com.algoprep.judge.application.dto.ExecutionModels.Verdict;
import com.algoprep.judge.application.dto.RecordPendingSubmissionCommand;
import com.algoprep.judge.application.dto.SampleRun;
import com.algoprep.judge.application.dto.SampleRun.Command;
import com.algoprep.judge.application.dto.SampleRun.Result;
import com.algoprep.judge.application.ports.in.EvaluateProgramUseCase;
import com.algoprep.judge.application.ports.in.RunSampleUseCase;
import com.algoprep.judge.application.ports.out.SampleQuotaPort;
import com.algoprep.judge.domain.exception.SampleRunException;
import com.algoprep.judge.domain.exception.SubmissionAdmissionException;
import com.algoprep.judge.domain.model.PendingSubmission.Owner;
import com.algoprep.judge.domain.model.PendingSubmission.Program;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.stereotype.Service;

/** No transaction spans sandbox I/O and no persisted submission is created. */
@Service
class RunSampleCommandHandler implements RunSampleUseCase {
  private final SampleRunContext context;
  private final EvaluateProgramUseCase evaluation;
  private final SampleQuotaPort quota;
  private final SampleRunBudget budget;

  RunSampleCommandHandler(
      SampleRunContext context,
      EvaluateProgramUseCase evaluation,
      SampleQuotaPort quota,
      SampleRunBudget budget) {
    this.context = context;
    this.evaluation = evaluation;
    this.quota = quota;
    this.budget = budget;
  }

  @Override
  public Result run(Command command) {
    var p =
        SubmissionAdmissionValidation.validate(
            new RecordPendingSubmissionCommand(
                new Owner(command.userId(), command.problemId()),
                new Program(
                    command.language(), command.submissionModel(), command.sourceCode(), "EDITOR"),
                null));
    var normalized =
        new Command(
            command.userId(),
            command.problemId(),
            p.language(),
            p.submissionModel(),
            p.sourceCode());
    return budget.call(
        () -> {
          try {
            return execute(normalized);
          } catch (SubmissionAdmissionException | SampleRunException known) {
            throw known;
          } catch (RuntimeException unavailable) {
            throw new SampleRunException("JDG-501");
          }
        });
  }

  private Result execute(Command command) {
    var data = context.load(command);
    UUID requestId = UUID.randomUUID();
    var reservation = quota.reserve(command.userId(), requestId);
    if (reservation.retryAfterSeconds() > 0) {
      throw new SampleRunException(
          "JDG-107", java.util.Map.of("retryAfterSeconds", reservation.retryAfterSeconds()));
    }
    boolean acquired = false;
    boolean charged = false;
    var refunded = new AtomicBoolean();
    Runnable refund =
        () -> {
          if (reservation.recorded() && refunded.compareAndSet(false, true)) {
            refund(command.userId(), requestId);
          }
        };
    SampleRunBudget.onCancel(refund);
    try {
      acquired = budget.acquire();
      if (!acquired) {
        throw new SampleRunException("JDG-502");
      }
      var result = evaluate(command, requestId, data);
      SampleRunBudget.check();
      charged = true;
      return result;
    } finally {
      if (acquired) {
        budget.release();
      }
      if (!charged) {
        refund.run();
      }
    }
  }

  private void refund(UUID userId, UUID requestId) {
    boolean interrupted = Thread.interrupted();
    try {
      quota.refund(userId, requestId);
    } finally {
      if (interrupted) {
        Thread.currentThread().interrupt();
      }
    }
  }

  private Result evaluate(Command command, UUID requestId, SampleRunContext.Loaded data) {
    var prepared = context.prepare(command, requestId, data);
    SampleRunBudget.check();
    var runLimits = SampleRunContext.limits(data);
    var compileLimits =
        new Limits(
            Math.multiplyExact(data.settings().execution().compileSeconds(), 1000L),
            1073741824L,
            65536,
            runLimits.processes());
    var cases =
        data.cases().stream()
            .map(
                c ->
                    new Testcase(
                        WorkerExecution.decode(c.input()),
                        WorkerExecution.decode(c.expectedOutput()),
                        true,
                        runLimits))
            .toList();
    var result =
        evaluation.evaluate(
            new EvaluationRequest(
                prepared.program(),
                compileLimits,
                prepared.matching(),
                cases,
                data.settings().execution().returnStderr()));
    if (result.verdict() == Verdict.SYSTEM_ERROR) {
      throw new SampleRunException("JDG-501");
    }
    if (result.verdict() == Verdict.COMPILE_ERROR) {
      return new Result("COMPILE_ERROR", result.compileMessage(), 0, 0, List.of());
    }
    var rows = new ArrayList<SampleRun.Case>();
    for (int i = 0; i < result.results().size(); i++) {
      rows.add(SampleRun.Case.of(data.cases().get(i).testcaseId(), result.results().get(i)));
    }
    return new Result(
        "COMPLETED",
        null,
        (int) rows.stream().filter(c -> c.verdict() == Verdict.AC).count(),
        rows.size(),
        rows);
  }
}
