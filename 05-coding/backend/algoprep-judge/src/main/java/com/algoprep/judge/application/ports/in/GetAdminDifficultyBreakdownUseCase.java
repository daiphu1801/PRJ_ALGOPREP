package com.algoprep.judge.application.ports.in;

import com.algoprep.judge.domain.model.AdminSubmissionStats;

/** Trusted admin caller in identity owns authorization, metadata and per-block caching. */
public interface GetAdminDifficultyBreakdownUseCase {
  AdminSubmissionStats.Difficulty byDifficulty(
      java.util.List<AdminSubmissionStats.LevelProblems> levels);
}
