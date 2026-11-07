package com.algoprep.problembank.application.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/** Response of the suggested problems; field names are the wire names. */
public final class SuggestedProblemViews {
  private SuggestedProblemViews() {}

  /** {@code solveState} is null when identity could not answer. */
  public record Item(
      UUID problemId,
      String code,
      String title,
      String levelCode,
      String levelDisplayName,
      String topicName,
      BigDecimal acRate,
      String solveState,
      boolean functionWrapperSupported) {}

  public record TopicScoreItem(UUID topicId, String topicName, int sortOrder, int score) {}

  /**
   * Section 13. {@code rankingAvailable} false means the scores could not be computed, which is not
   * the same as true with an empty {@code topicScores} (no submissions yet).
   */
  public record SuggestedProblems(
      List<Item> items, List<TopicScoreItem> topicScores, boolean rankingAvailable) {}
}
