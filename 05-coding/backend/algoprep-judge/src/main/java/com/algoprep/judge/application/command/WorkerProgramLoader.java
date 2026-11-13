package com.algoprep.judge.application.command;

import com.algoprep.common.exception.DomainRuleViolationException;
import com.algoprep.judge.application.dto.ExecutionModels.Limits;
import com.algoprep.judge.application.dto.GradingSpecification;
import com.algoprep.judge.application.dto.WorkerPreparation;
import com.algoprep.judge.application.ports.out.ProblemGradingSpecPort;
import com.algoprep.judge.application.ports.out.WorkerProgramPort;
import com.algoprep.judge.domain.exception.ProblemGradingReadException;
import com.algoprep.judge.domain.model.WorkerJob;
import com.algoprep.judge.domain.model.WorkerSettings;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.stereotype.Component;

@Component
class WorkerProgramLoader {
  private final ProblemGradingSpecPort problems;
  private final WorkerProgramPort programs;

  WorkerProgramLoader(ProblemGradingSpecPort problems, WorkerProgramPort programs) {
    this.problems = problems;
    this.programs = programs;
  }

  GradingSpecification spec(WorkerJob job) {
    return WorkerRetry.read(
        () ->
            problems
                .loadGradingSpec(job.problemId())
                .orElseThrow(
                    () ->
                        new ProblemGradingReadException(
                            ProblemGradingReadException.Reason.SPEC_UNAVAILABLE)));
  }

  WorkerPreparation.Prepared prepare(WorkerJob job, GradingSpecification spec) {
    for (int attempt = 0; ; attempt++) {
      try {
        return programs.prepare(new WorkerPreparation.Request(job, spec.versions().specVersion()));
      } catch (DomainRuleViolationException e) {
        if (!e.errorCode().equals("HRN-003") || attempt == 2) {
          throw e;
        }
        WorkerRetry.pause(attempt);
      }
    }
  }

  Limits runLimits(GradingSpecification spec, WorkerSettings settings) {
    var limits = spec.limits();
    long time =
        scaled(
            limits.timeLimitMs() > 0 ? limits.timeLimitMs() : settings.defaultTimeMs(),
            settings.timeMultiplier());
    long memory =
        scaled(
            limits.memoryLimitMb() > 0 ? limits.memoryLimitMb() : settings.defaultMemoryMb(),
            settings.memoryMultiplier());
    int output =
        limits.maxOutputSizeKb() > 0 ? limits.maxOutputSizeKb() : settings.defaultOutputKb();
    return new Limits(
        time,
        Math.multiplyExact(memory, 1048576L),
        Math.multiplyExact(output, 1024L),
        settings.processLimitEnabled() ? 60 : 0);
  }

  private long scaled(int base, BigDecimal multiplier) {
    return BigDecimal.valueOf(base)
        .multiply(multiplier)
        .setScale(0, RoundingMode.HALF_UP)
        .longValueExact();
  }
}
