package com.algoprep.judge.application.query;

import com.algoprep.judge.application.ports.in.GetEnabledJudgeLanguagesUseCase;
import com.algoprep.judge.application.ports.in.GetGeneratorSandboxLimitsUseCase;
import com.algoprep.judge.application.ports.in.GetLanguageMultipliersUseCase;
import com.algoprep.judge.domain.exception.JudgeReadUnavailableException;
import com.algoprep.judge.domain.model.GeneratorSandboxLimits;
import com.algoprep.judge.domain.model.LanguageMultipliers;
import com.algoprep.judge.domain.ports.out.JudgeConfigRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
class GetJudgeConfigQueryHandler
    implements GetGeneratorSandboxLimitsUseCase,
        GetEnabledJudgeLanguagesUseCase,
        GetLanguageMultipliersUseCase {
  private final JudgeConfigRepository repository;

  GetJudgeConfigQueryHandler(JudgeConfigRepository repository) {
    this.repository = repository;
  }

  @Override
  public GeneratorSandboxLimits getGeneratorSandboxLimits() {
    try {
      return java.util.Objects.requireNonNull(repository.generatorLimits());
    } catch (RuntimeException failure) {
      throw new JudgeReadUnavailableException();
    }
  }

  @Override
  public List<String> getEnabledLanguages() {
    try {
      return List.copyOf(repository.enabledLanguages());
    } catch (RuntimeException failure) {
      throw new JudgeReadUnavailableException();
    }
  }

  @Override
  public LanguageMultipliers getLanguageMultipliers(String language) {
    if (!"JAVA".equals(language) && !"CPP".equals(language) && !"PYTHON".equals(language)) {
      throw new IllegalArgumentException("Unsupported judge language");
    }
    try {
      return repository.multipliers(language).orElseThrow(JudgeReadUnavailableException::new);
    } catch (RuntimeException failure) {
      throw new JudgeReadUnavailableException();
    }
  }
}
