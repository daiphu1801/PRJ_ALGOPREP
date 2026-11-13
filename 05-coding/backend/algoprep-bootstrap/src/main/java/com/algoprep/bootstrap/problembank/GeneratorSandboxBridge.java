package com.algoprep.bootstrap.problembank;

import com.algoprep.bootstrap.judge.HarnessProgramBridge;
import com.algoprep.judge.application.dto.ExecutionModels.CompileRequest;
import com.algoprep.judge.application.dto.ExecutionModels.Limits;
import com.algoprep.judge.application.dto.ExecutionModels.RunRequest;
import com.algoprep.judge.application.dto.ExecutionModels.RunResult;
import com.algoprep.judge.application.dto.ExecutionModels.Status;
import com.algoprep.judge.application.dto.SampleRun;
import com.algoprep.judge.application.dto.WorkerPreparation.Prepared;
import com.algoprep.judge.application.ports.in.GetLanguageMultipliersUseCase;
import com.algoprep.judge.application.ports.out.JudgeExecutionPort;
import com.algoprep.judge.application.ports.out.SampleProgramPort;
import com.algoprep.problembank.application.dto.ReferenceSolutionRun;
import com.algoprep.problembank.application.dto.TestcaseGeneration.GeneratorRun;
import com.algoprep.problembank.application.dto.TestcaseGeneration.ReferenceOutput;
import com.algoprep.problembank.application.dto.TestcaseGeneration.ReferenceStatus;
import com.algoprep.problembank.application.ports.out.GeneratorSandboxPort;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Sandbox adapter of BR-16 for problem-bank: the scripts go to {@link GeneratorScriptRunner}, the
 * sample solution runs through the harness and the judge exactly like a learner Run Code, except
 * that the real stdout of each case is read back instead of a verdict. Any sandbox fault is an
 * unchecked exception, which the caller turns into 503 PRB-050.
 */
@Component
final class GeneratorSandboxBridge implements GeneratorSandboxPort {
  private static final UUID NO_LEARNER = new UUID(0, 0);
  private static final int PROCESSES = 60;
  private static final long COMPILE_MEMORY_BYTES = 1_073_741_824L;
  private static final int COMPILE_OUTPUT_BYTES = 65_536;

  private final GeneratorScriptRunner scripts;
  private final SampleProgramPort programs;
  private final JudgeExecutionPort engine;
  private final ReferenceConversions conversions;

  /** Pieces only the reference run needs, grouped to keep the constructor short. */
  @Component
  record ReferenceConversions(
      HarnessProgramBridge harness, GetLanguageMultipliersUseCase multipliers) {}

  GeneratorSandboxBridge(
      GeneratorScriptRunner scripts,
      SampleProgramPort programs,
      JudgeExecutionPort engine,
      ReferenceConversions conversions) {
    this.scripts = scripts;
    this.programs = programs;
    this.engine = engine;
    this.conversions = conversions;
  }

  @Override
  public GeneratorRun runGenerator(String script, long seed) {
    return scripts.generate(script, seed);
  }

  @Override
  public List<Boolean> validate(String script, List<String> inputs) {
    return scripts.validate(script, inputs);
  }

  @Override
  public List<ReferenceOutput> runReference(ReferenceSolutionRun.Request request) {
    var scale = conversions.multipliers().getLanguageMultipliers(request.language());
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
    var outputs = new ArrayList<ReferenceOutput>();
    try (var execution = engine.compile(new CompileRequest(prepared.program(), compile))) {
      if (!execution.compilation().success()) {
        throw new IllegalStateException("Sample solution no longer compiles");
      }
      for (var one : request.cases()) {
        var result = execution.run(new RunRequest(one.input(), run));
        outputs.add(toReference(result, prepared));
      }
    }
    return outputs;
  }

  private ReferenceOutput toReference(RunResult result, Prepared prepared) {
    if (result.status() != Status.OK) {
      return new ReferenceOutput(
          result.status() == Status.TLE ? ReferenceStatus.TIMEOUT : ReferenceStatus.FAILED, null);
    }
    var match =
        conversions.harness().match(prepared.program(), prepared.matching(), result.stdout(), "");
    return match.actualOutput() == null
        ? new ReferenceOutput(ReferenceStatus.FAILED, null)
        : new ReferenceOutput(ReferenceStatus.OK, match.actualOutput());
  }

  private static long scaled(long base, BigDecimal multiplier) {
    return BigDecimal.valueOf(base)
        .multiply(multiplier)
        .setScale(0, RoundingMode.HALF_UP)
        .longValueExact();
  }
}
