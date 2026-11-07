package com.algoprep.problembank.domain.model;

import java.util.UUID;

/** Read models of the statistics gate that identity reads for the admin overview (DD F2 58.1). */
public final class ProblemBankStatsModels {
  private ProblemBankStatsModels() {}

  public record LevelRef(UUID id, String code, String displayName, int sortOrder) {}

  /**
   * {@code published} is {@code PUBLISHED AND NOT deleted}: the only problems the overview counts.
   */
  public record ProblemStat(
      UUID problemId,
      String title,
      UUID levelId,
      String levelCode,
      String levelDisplayName,
      boolean published) {}
}
