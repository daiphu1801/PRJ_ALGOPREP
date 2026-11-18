package com.algoprep.problembank.application.ports.out;

import com.algoprep.problembank.application.dto.grading.JudgeGradingDefaults;

/** F2-owned subset of the judge configuration contract (DD F4 section 26.3). */
public interface JudgeConfigReadPort {
  JudgeGradingDefaults getGradingDefaults();
}
