package com.algoprep.judge.application.ports.in;

import com.algoprep.judge.domain.model.LanguageMultipliers;

public interface GetLanguageMultipliersUseCase {
  LanguageMultipliers getLanguageMultipliers(String language);
}
