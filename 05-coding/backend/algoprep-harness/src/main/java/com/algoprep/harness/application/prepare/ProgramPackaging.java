package com.algoprep.harness.application.prepare;

import com.algoprep.common.functionspec.FunctionSpecDraft;
import com.algoprep.common.functionspec.FunctionSpecEvaluator;
import com.algoprep.common.functionspec.Language;
import com.algoprep.harness.application.dto.PackagedProgram;
import com.algoprep.harness.application.dto.SubmissionModel;
import com.algoprep.harness.application.ports.out.JudgePayloadAssembler;
import com.algoprep.harness.domain.model.RenderedProgram;
import com.algoprep.harness.domain.signature.FunctionSignatureDeclarations;
import java.util.Objects;
import org.springframework.stereotype.Service;

/** Packages caller-supplied typed input; never reads or simulates problem-bank data. */
@Service
public final class ProgramPackaging {
  private final JudgePayloadAssembler assembler;

  public ProgramPackaging(JudgePayloadAssembler assembler) {
    this.assembler = assembler;
  }

  public PackagedProgram functionWrapper(
      Language language, FunctionSpecDraft draft, String source) {
    Objects.requireNonNull(language);
    var evaluation = FunctionSpecEvaluator.evaluate(Objects.requireNonNull(draft));
    if (!evaluation.violations().isEmpty() || !evaluation.wrapperSupported()) {
      throw new IllegalArgumentException("Invalid or unsupported function specification");
    }
    var rendered = new FunctionWrapperRenderer().render(language, draft, source);
    return pack(
        language,
        SubmissionModel.FUNCTION_WRAPPER,
        rendered,
        FunctionSignatureDeclarations.render(draft).get(language));
  }

  public PackagedProgram standardIo(Language language, String source) {
    return pack(
        language,
        SubmissionModel.STANDARD_IO,
        new ProgramSourceAssembler().standardIo(language, source),
        null);
  }

  private PackagedProgram pack(
      Language language, SubmissionModel model, RenderedProgram source, String signature) {
    var plan = new ToolchainProfile().plan(language, model, source);
    return new PackagedProgram(
        language,
        model,
        assembler.assemble(plan),
        source.lineMap(),
        signature,
        model == SubmissionModel.FUNCTION_WRAPPER ? "@@APH-RESULT@@" : null);
  }
}
