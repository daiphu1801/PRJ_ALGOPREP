package com.algoprep.judge.domain.ports.out;

import com.algoprep.judge.domain.model.grading.GeneratorSandboxLimits;
import com.algoprep.judge.domain.model.language.LanguageMultipliers;
import java.util.List;
import java.util.Optional;

public interface JudgeConfigRepository {
  GeneratorSandboxLimits generatorLimits();

  List<String> enabledLanguages();

  Optional<LanguageMultipliers> multipliers(String language);
}
