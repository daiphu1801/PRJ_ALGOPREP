package com.algoprep.problembank.application.query;

import com.algoprep.problembank.application.dto.AuthoringDefaults;
import com.algoprep.common.exception.ServiceUnavailableException;
import com.algoprep.problembank.application.ports.in.ReadAuthoringDefaultsUseCase;
import com.algoprep.problembank.application.ports.out.JudgeConfigReadPort;
import com.algoprep.problembank.domain.ports.out.ProblemAuthoringReadRepository;
import org.springframework.stereotype.Service;

@Service
class ReadAuthoringDefaultsQueryHandler implements ReadAuthoringDefaultsUseCase {
  private final JudgeConfigReadPort judge;
  private final ProblemAuthoringReadRepository repository;

  ReadAuthoringDefaultsQueryHandler(
      JudgeConfigReadPort judge, ProblemAuthoringReadRepository repository) {
    this.judge = judge;
    this.repository = repository;
  }

  @Override
  public AuthoringDefaults get() {
    try {
      var limits = judge.getGradingDefaults();
      return new AuthoringDefaults(limits.timeLimitMs(), limits.memoryLimitMb(), limits.maxOutputSizeKb(), repository.aiGenerationLimit());
    } catch (RuntimeException unavailable) {
      throw new ServiceUnavailableException("PRB-050", "Authoring defaults unavailable", null);
    }
  }
}
