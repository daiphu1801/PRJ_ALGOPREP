package com.algoprep.judge.application.ports.in.grading;

import com.algoprep.judge.domain.model.grading.GradingDefaults;

/** Internal read contract. Unavailable configuration is an error, never fabricated defaults. */
public interface ReadGradingDefaultsUseCase {
  GradingDefaults getGradingDefaults();
}
