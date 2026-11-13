package com.algoprep.judge.application.ports.in;

import com.algoprep.judge.domain.model.GradingDefaults;

/** Internal read contract. Unavailable configuration is an error, never fabricated defaults. */
public interface ReadGradingDefaultsUseCase {
  GradingDefaults getGradingDefaults();
}
