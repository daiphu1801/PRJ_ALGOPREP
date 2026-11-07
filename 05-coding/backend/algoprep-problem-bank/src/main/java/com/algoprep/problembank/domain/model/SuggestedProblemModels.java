package com.algoprep.problembank.domain.model;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/** Read models of the suggested problems of the dashboard (DD F2 section 13). */
public final class SuggestedProblemModels {
  private SuggestedProblemModels() {}

  /** A published problem that matches the filter, with every topic it belongs to. */
  public record Candidate(
      UUID id,
      String code,
      String title,
      String levelCode,
      String levelDisplayName,
      List<String> topicNames,
      List<UUID> topicIds,
      BigDecimal acRate,
      boolean functionWrapperSupported) {}

  /** The caller's all-time submission counts over the problems of one topic (judge P5). */
  public record TopicMetric(long submissionCount, long acceptedCount, Double acRate) {}

  /** Score 0 to 100 of one topic; a topic without submissions has no score at all. */
  public record TopicScore(UUID topicId, String topicName, int sortOrder, int score) {}
}
