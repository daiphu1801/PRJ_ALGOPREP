package com.algoprep.bootstrap.problembank;

import com.algoprep.judge.application.dto.ExecutionModels.EvaluationRequest;
import com.algoprep.judge.application.dto.ExecutionModels.EvaluationResult;
import com.algoprep.judge.application.dto.ExecutionModels.Limits;
import com.algoprep.judge.application.dto.ExecutionModels.Testcase;
import com.algoprep.judge.application.dto.ExecutionModels.Verdict;
import com.algoprep.judge.application.dto.SampleRun;
import com.algoprep.judge.application.ports.in.EvaluateProgramUseCase;
import com.algoprep.judge.application.ports.in.GetLanguageMultipliersUseCase;
import com.algoprep.judge.application.ports.out.SampleProgramPort;
import com.algoprep.problembank.application.dto.ReferenceSolutionRun;
import com.algoprep.problembank.application.ports.out.SampleSolutionRunnerPort;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Runs the reference solution of a (possibly unpublished) problem the way a learner's Run Code
 * does: the harness wraps the code, the judge compiles once and runs every case in the sandbox.
 * Language multipliers scale the stored limits exactly as for a learner.
 *
 * <p>ponytail: the process limit is fixed at 60 (the "limit on" value of the per-language setting
 * the judge keeps), because the judge exposes no inbound read of that flag; expose it if a language
 * is ever configured without one.
 */
@Component
final class SampleSolutionRunnerBridge implements SampleSolutionRunnerPort {
  private static final UUID NO_LEARNER = new UUID(0, 0);
  private static final int PROCESSES = 60;
  private static final long COMPILE_MEMORY_BYTES = 1_073_741_824L;
  private static final int COMPILE_OUTPUT_BYTES = 65_536;

  private final SampleProgramPort programs;
  private final EvaluateProgramUseCase evaluation;
  private final GetLanguageMultipliersUseCase multipliers;

  SampleSolutionRunnerBridge(
      SampleProgramPort programs,
      EvaluateProgramUseCase evaluation,
      GetLanguageMultipliersUseCase multipliers) {
    this.programs = programs;
    this.evaluation = evaluation;
    this.multipliers = multipliers;
  }

  @Override
  public ReferenceSolutionRun.Result run(ReferenceSolutionRun.Request request) {
    var scale = multipliers.getLanguageMultipliers(request.language());
    var base = request.limits();
    var run =
        new Limits(
            scaled(base.timeLimitMs(), scale.timeMultiplier()),
            scaled(base.memoryLimitMb(), scale.memoryMultiplier()) * 1_048_576L,
            base.maxOutputSizeKb() * 1024L,
            PROCESSES);
    var compile =
        new Limits(
            base.compileTimeoutSeconds() * 1000L,
            COMPILE_MEMORY_BYTES,
            COMPILE_OUTPUT_BYTES,
            PROCESSES);
    var model = request.functionWrapper() ? "FUNCTION_WRAPPER" : "STANDARD_IO";
    var prepared =
        programs.prepare(
            new SampleRun.Preparation(
                new SampleRun.Command(
                    NO_LEARNER,
                    request.problemId(),
                    request.language(),
                    model,
                    request.sourceCode()),
                UUID.randomUUID(),
                request.specVersion()));
    var cases =
        request.cases().stream()
            .map(c -> new Testcase(c.input(), c.expectedOutput(), true, run))
            .toList();
    var result =
        evaluation.evaluate(
            new EvaluationRequest(prepared.program(), compile, prepared.matching(), cases, false));
    if (result.verdict() == Verdict.SYSTEM_ERROR) {
      throw new IllegalStateException("Sandbox unavailable");
    }
    return map(request, result);
  }

  private static ReferenceSolutionRun.Result map(
      ReferenceSolutionRun.Request request, EvaluationResult result) {
    if (result.verdict() == Verdict.COMPILE_ERROR) {
      return new ReferenceSolutionRun.Result(true, java.util.List.of(), result.compileMessage());
    }
    var rows = new ArrayList<ReferenceSolutionRun.CaseResult>();
    for (int i = 0; i < result.results().size(); i++) {
      var one = result.results().get(i);
      rows.add(
          new ReferenceSolutionRun.CaseResult(
              request.cases().get(i).testcaseId(),
              one.verdict() == Verdict.AC,
              one.verdict().name(),
              one.runtimeMs(),
              one.memoryKb()));
    }
    return new ReferenceSolutionRun.Result(false, rows);
  }

  private static long scaled(long base, BigDecimal multiplier) {
    return BigDecimal.valueOf(base)
        .multiply(multiplier)
        .setScale(0, RoundingMode.HALF_UP)
        .longValueExact();
  }
}
