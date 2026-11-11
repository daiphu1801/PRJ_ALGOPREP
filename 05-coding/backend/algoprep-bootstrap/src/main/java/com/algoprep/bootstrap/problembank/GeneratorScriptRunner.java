package com.algoprep.bootstrap.problembank;

import com.algoprep.bootstrap.judge.HarnessProgramBridge;
import com.algoprep.common.functionspec.Language;
import com.algoprep.harness.application.prepare.ProgramPackaging;
import com.algoprep.judge.application.dto.ExecutionModels.CompileRequest;
import com.algoprep.judge.application.dto.ExecutionModels.Limits;
import com.algoprep.judge.application.dto.ExecutionModels.Program;
import com.algoprep.judge.application.dto.ExecutionModels.RunRequest;
import com.algoprep.judge.application.dto.ExecutionModels.Status;
import com.algoprep.judge.application.ports.in.GetGeneratorSandboxLimitsUseCase;
import com.algoprep.judge.application.ports.out.JudgeExecutionPort;
import com.algoprep.problembank.application.dto.TestcaseGeneration.GeneratorRun;
import com.algoprep.problembank.application.dto.TestcaseGeneration.ScriptStatus;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Runs the two untrusted Python scripts of BR-16 (generator and validator) as plain standard-I/O
 * programs in the same sandbox that judges submissions. The seed reaches the generator through the
 * SEED environment variable, set by two lines prepended to the script because the sandbox request
 * carries stdin only.
 *
 * <p>ponytail: the judge reports "Output Limit Exceeded" as a runtime error, so OUTPUT_TOO_LARGE is
 * recognised by a failed run whose stdout filled the limit; give the sandbox a distinct status if
 * that guess ever misfires.
 */
@Component
final class GeneratorScriptRunner {
  static final int FALLBACK_SECONDS = 30;
  static final int FALLBACK_OUTPUT_MB = 100;
  private static final Logger LOG = LoggerFactory.getLogger(GeneratorScriptRunner.class);
  private static final long MEMORY_BYTES = 536_870_912L;
  private static final int PROCESSES = 60;
  private static final Limits COMPILE = new Limits(10_000, 1_073_741_824L, 65_536, PROCESSES);
  private static final Limits VALIDATE = new Limits(5_000, MEMORY_BYTES, 65_536, PROCESSES);

  private final ProgramPackaging packaging;
  private final HarnessProgramBridge conversion;
  private final JudgeExecutionPort engine;
  private final GetGeneratorSandboxLimitsUseCase limits;

  GeneratorScriptRunner(
      ProgramPackaging packaging,
      HarnessProgramBridge conversion,
      JudgeExecutionPort engine,
      GetGeneratorSandboxLimitsUseCase limits) {
    this.packaging = packaging;
    this.conversion = conversion;
    this.engine = engine;
    this.limits = limits;
  }

  GeneratorRun generate(String script, long seed) {
    int seconds = FALLBACK_SECONDS;
    int megabytes = FALLBACK_OUTPUT_MB;
    try {
      var configured = limits.getGeneratorSandboxLimits();
      seconds = configured.maxRuntimeSeconds();
      megabytes = configured.maxOutputMb();
    } catch (RuntimeException unavailable) {
      LOG.warn("Generator sandbox limits unavailable; using the fallback constants");
    }
    var run = new Limits(seconds * 1000L, MEMORY_BYTES, megabytes * 1_048_576L, PROCESSES);
    var source = "import os\nos.environ[\"SEED\"] = \"" + seed + "\"\n" + script;
    try (var execution = engine.compile(new CompileRequest(program(source), COMPILE))) {
      if (!execution.compilation().success()) {
        return new GeneratorRun(ScriptStatus.SCRIPT_ERROR, null);
      }
      var result = execution.run(new RunRequest("", run));
      return new GeneratorRun(map(result.status(), result.stdout(), run), result.stdout());
    }
  }

  List<Boolean> validate(String script, List<String> inputs) {
    var flags = new ArrayList<Boolean>();
    try (var execution = engine.compile(new CompileRequest(program(script), COMPILE))) {
      if (!execution.compilation().success()) {
        inputs.forEach(i -> flags.add(false));
        return flags;
      }
      for (String input : inputs) {
        flags.add(execution.run(new RunRequest(input, VALIDATE)).status() == Status.OK);
      }
    }
    return flags;
  }

  private Program program(String source) {
    return conversion.program(packaging.standardIo(Language.PYTHON, source));
  }

  private static ScriptStatus map(Status status, String stdout, Limits run) {
    return switch (status) {
      case OK -> ScriptStatus.OK;
      case TLE -> ScriptStatus.TIMEOUT;
      case RE ->
          stdout != null && stdout.length() >= run.outputBytes() * 0.99
              ? ScriptStatus.OUTPUT_TOO_LARGE
              : ScriptStatus.SCRIPT_ERROR;
      default -> ScriptStatus.SCRIPT_ERROR;
    };
  }
}
