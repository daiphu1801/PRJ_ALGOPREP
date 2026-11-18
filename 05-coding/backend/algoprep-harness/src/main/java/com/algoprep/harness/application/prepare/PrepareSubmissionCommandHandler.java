package com.algoprep.harness.application.prepare;

import com.algoprep.common.exception.DomainRuleViolationException;
import com.algoprep.harness.application.dto.MatchSpec;
import com.algoprep.harness.application.dto.PrepareSubmissionCommand;
import com.algoprep.harness.application.dto.PreparedProgram;
import com.algoprep.harness.application.dto.SubmissionModel;
import com.algoprep.harness.application.ports.in.PrepareSubmissionUseCase;
import com.algoprep.harness.application.query.CurrentProblemSpecification;

/** P2 reads once, pins the spec version, then invokes the existing deterministic packaging core. */
public class PrepareSubmissionCommandHandler implements PrepareSubmissionUseCase {
  private final CurrentProblemSpecification specs;
  private final ProgramPackaging packaging;

  public PrepareSubmissionCommandHandler(
      CurrentProblemSpecification specs, ProgramPackaging packaging) {
    this.specs = specs;
    this.packaging = packaging;
  }

  @Override
  public PreparedProgram prepare(PrepareSubmissionCommand command) {
    if (command == null) {
      throw new DomainRuleViolationException("HRN-031", "Invalid preparation request");
    }
    new ProgramSourceAssembler().standardIo(command.language(), command.sourceCode());
    if (command.model() == null) {
      throw new DomainRuleViolationException("HRN-031", "Missing submission model");
    }
    var spec = specs.load(command.problemId());
    if (command.expectedSpecVersion() != null
        && command.expectedSpecVersion() != spec.specVersion()) {
      throw new DomainRuleViolationException("HRN-002", "Problem specification version changed");
    }
    boolean wrapper = command.model() == SubmissionModel.FUNCTION_WRAPPER;
    if (wrapper) {
      specs.validateWrapper(spec);
    }
    var program =
        wrapper
            ? packaging.functionWrapper(command.language(), spec.signature(), command.sourceCode())
            : packaging.standardIo(command.language(), command.sourceCode());
    var draft = spec.signature();
    return new PreparedProgram(
        program.language(),
        program.model(),
        spec.specVersion(),
        program.payload(),
        program.lineMap(),
        program.signatureText(),
        new MatchSpec(
            draft.matchingStrategy(), draft.epsilonValue(), wrapper ? draft.returnType() : null),
        program.resultSentinel(),
        90);
  }
}
