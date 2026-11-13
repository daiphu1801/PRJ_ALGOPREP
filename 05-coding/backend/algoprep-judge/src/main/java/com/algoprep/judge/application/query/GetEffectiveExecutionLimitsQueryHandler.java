package com.algoprep.judge.application.query;

import com.algoprep.judge.application.dto.ExecutionLimitsView;
import com.algoprep.judge.application.ports.in.GetEffectiveExecutionLimitsUseCase;
import com.algoprep.judge.application.ports.out.ProblemGradingSpecPort;
import com.algoprep.judge.domain.exception.SubmissionAdmissionException;
import com.algoprep.judge.domain.ports.out.SampleSettingsRepository;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class GetEffectiveExecutionLimitsQueryHandler implements GetEffectiveExecutionLimitsUseCase {
  private final ProblemGradingSpecPort problems;
  private final SampleSettingsRepository settings;

  GetEffectiveExecutionLimitsQueryHandler(
      ProblemGradingSpecPort problems, SampleSettingsRepository settings) {
    this.problems = problems;
    this.settings = settings;
  }

  @Override
  @Transactional(readOnly = true)
  public ExecutionLimitsView get(UUID problemId, String language) {
    String selected = language == null ? null : language.toUpperCase(Locale.ROOT);
    if (selected != null && !Set.of("JAVA", "CPP", "PYTHON").contains(selected)) {
      throw new SubmissionAdmissionException("JDG-V03");
    }
    var spec =
        problems
            .loadGradingSpec(problemId)
            .filter(s -> s.problem().published())
            .orElseThrow(() -> new SubmissionAdmissionException("JDG-101"));
    var base = spec.limits();
    var limits =
        settings.read().stream()
            .filter(s -> selected == null || s.language().equals(selected))
            .map(s -> s.effective(base.timeLimitMs(), base.memoryLimitMb(), base.maxOutputSizeKb()))
            .toList();
    if (limits.size() != (selected == null ? 3 : 1)) {
      throw new IllegalStateException("Execution configuration unavailable");
    }
    return new ExecutionLimitsView(problemId, limits);
  }
}
