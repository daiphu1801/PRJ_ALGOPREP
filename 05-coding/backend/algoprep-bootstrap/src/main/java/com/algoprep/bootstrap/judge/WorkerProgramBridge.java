package com.algoprep.bootstrap.judge;

import com.algoprep.common.functionspec.Language;
import com.algoprep.harness.application.dto.PackagedProgram;
import com.algoprep.harness.application.dto.PrepareSubmissionCommand;
import com.algoprep.harness.application.dto.SubmissionModel;
import com.algoprep.harness.application.ports.in.PrepareSubmissionUseCase;
import com.algoprep.judge.application.dto.ExecutionModels.Matching;
import com.algoprep.judge.application.dto.WorkerPreparation.Prepared;
import com.algoprep.judge.application.dto.WorkerPreparation.Request;
import com.algoprep.judge.application.ports.out.WorkerProgramPort;
import org.springframework.stereotype.Component;

@Component
final class WorkerProgramBridge implements WorkerProgramPort {
  private final PrepareSubmissionUseCase prepare;
  private final HarnessProgramBridge conversion;

  WorkerProgramBridge(PrepareSubmissionUseCase prepare, HarnessProgramBridge conversion) {
    this.prepare = prepare;
    this.conversion = conversion;
  }

  @Override
  public Prepared prepare(Request request) {
    var job = request.job();
    var p =
        prepare.prepare(
            new PrepareSubmissionCommand(
                job.problemId(),
                Language.valueOf(job.language()),
                SubmissionModel.valueOf(job.model()),
                job.sourceCode(),
                request.specVersion(),
                job.id().toString()));
    var packaged =
        new PackagedProgram(
            p.language(),
            p.model(),
            p.payload(),
            p.lineMap(),
            p.signatureText(),
            p.resultSentinel());
    return new Prepared(
        conversion.program(packaged),
        new Matching(
            p.matchSpec().matchingStrategy(),
            p.matchSpec().epsilonValue(),
            p.matchSpec().returnType()));
  }
}
