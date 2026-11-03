package com.algoprep.judge.domain.ports.out;

import com.algoprep.judge.domain.model.GradingDefaults;

/** Reads the singleton owned by judge. */
public interface GradingDefaultsRepository {
  GradingDefaults read();
}
