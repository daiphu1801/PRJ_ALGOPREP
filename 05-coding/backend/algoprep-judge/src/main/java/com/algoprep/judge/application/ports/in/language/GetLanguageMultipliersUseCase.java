package com.algoprep.judge.application.ports.in.language;

import com.algoprep.judge.domain.model.language.LanguageMultipliers;

public interface GetLanguageMultipliersUseCase {
  LanguageMultipliers getLanguageMultipliers(String language);
}
