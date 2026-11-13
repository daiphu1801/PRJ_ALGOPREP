package com.algoprep.judge.application.command;

import com.algoprep.judge.application.dto.ExecutionModels.Limits;
import com.algoprep.judge.application.dto.GradingSpecification;
import com.algoprep.judge.application.dto.GradingTestcase;
import com.algoprep.judge.application.dto.SampleRun.Command;
import com.algoprep.judge.application.dto.SampleRun.Preparation;
import com.algoprep.judge.application.dto.WorkerPreparation.Prepared;
import com.algoprep.judge.application.ports.out.ProblemGradingSpecPort;
import com.algoprep.judge.application.ports.out.SampleProgramPort;
import com.algoprep.judge.domain.exception.SampleRunException;
import com.algoprep.judge.domain.exception.SubmissionAdmissionException;
import com.algoprep.judge.domain.model.SampleSettings;
import com.algoprep.judge.domain.ports.out.SampleSettingsRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
class SampleRunContext {
  private final ProblemGradingSpecPort problems;
  private final SampleSettingsRepository settings;
  private final SampleProgramPort programs;

  SampleRunContext(
      ProblemGradingSpecPort problems,
      SampleSettingsRepository settings,
      SampleProgramPort programs) {
    this.problems = problems;
    this.settings = settings;
    this.programs = programs;
  }

  record Loaded(GradingSpecification spec, SampleSettings settings, List<GradingTestcase> cases) {}

  Loaded load(Command command) {
    SampleRunBudget.check();
    var spec =
        problems
            .loadGradingSpec(command.problemId())
            .filter(s -> s.problem().published())
            .orElseThrow(() -> new SubmissionAdmissionException("JDG-101"));
    if (command.submissionModel().equals("FUNCTION_WRAPPER")
        && !spec.problem().functionWrapperSupported()) {
      throw new SubmissionAdmissionException("JDG-103");
    }
    var config =
        settings.read().stream()
            .filter(s -> s.language().equals(command.language()))
            .findFirst()
            .orElseThrow(() -> new SampleRunException("JDG-501"));
    if (!config.enabled()) {
      throw new SubmissionAdmissionException("JDG-102");
    }
    if (config.execution().networkEnabled()) {
      throw new SampleRunException("JDG-501");
    }
    var cases = problems.loadSampleTestcases(command.problemId());
    if (cases.isEmpty()) {
      throw new SubmissionAdmissionException("JDG-105");
    }
    if (cases.stream().anyMatch(c -> !"SAMPLE".equals(c.visibility()))) {
      throw new SampleRunException("JDG-501");
    }
    SampleRunBudget.check();
    return new Loaded(spec, config, List.copyOf(cases));
  }

  Prepared prepare(Command command, UUID requestId, Loaded data) {
    SampleRunBudget.check();
    return programs.prepare(
        new Preparation(command, requestId, data.spec().versions().specVersion()));
  }

  static Limits limits(Loaded data) {
    var base = data.spec().limits();
    var e =
        data.settings().effective(base.timeLimitMs(), base.memoryLimitMb(), base.maxOutputSizeKb());
    return new Limits(
        e.timeLimitMs(),
        Math.multiplyExact(e.memoryLimitMb(), 1048576L),
        Math.multiplyExact(e.maxOutputSizeKb(), 1024L),
        data.settings().execution().processLimitEnabled() ? 60 : 0);
  }
}
