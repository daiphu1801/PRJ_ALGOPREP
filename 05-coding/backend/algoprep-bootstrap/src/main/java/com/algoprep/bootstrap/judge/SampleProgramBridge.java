package com.algoprep.bootstrap.judge;

import com.algoprep.common.functionspec.Language;
import com.algoprep.harness.application.dto.PackagedProgram;
import com.algoprep.harness.application.dto.PrepareSubmissionCommand;
import com.algoprep.harness.application.dto.SubmissionModel;
import com.algoprep.harness.application.ports.in.PrepareSubmissionUseCase;
import com.algoprep.judge.application.dto.ExecutionModels.Matching;
import com.algoprep.judge.application.dto.SampleRun.Preparation;
import com.algoprep.judge.application.dto.WorkerPreparation.Prepared;
import com.algoprep.judge.application.ports.out.SampleProgramPort;
import org.springframework.stereotype.Component;

@Component
final class SampleProgramBridge implements SampleProgramPort {
  private final PrepareSubmissionUseCase prepare;
  private final HarnessProgramBridge conversion;

  SampleProgramBridge(PrepareSubmissionUseCase prepare, HarnessProgramBridge conversion) {
    this.prepare = prepare;
    this.conversion = conversion;
  }

  @Override
  public Prepared prepare(Preparation request) {
    var command = request.command();
    var p =
        prepare.prepare(
            new PrepareSubmissionCommand(
                command.problemId(),
                Language.valueOf(command.language()),
                SubmissionModel.valueOf(command.submissionModel()),
                command.sourceCode(),
                request.specVersion(),
                request.requestId().toString()));
    return new Prepared(
        conversion.program(
            new PackagedProgram(
                p.language(),
                p.model(),
                p.payload(),
                p.lineMap(),
                p.signatureText(),
                p.resultSentinel())),
        new Matching(
            p.matchSpec().matchingStrategy(),
            p.matchSpec().epsilonValue(),
            p.matchSpec().returnType()));
  }
}
