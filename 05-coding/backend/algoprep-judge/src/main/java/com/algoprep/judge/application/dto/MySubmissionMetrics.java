package com.algoprep.judge.application.dto;

import com.algoprep.judge.domain.model.SubmissionActivityDay;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record MySubmissionMetrics(
    int streakDays,
    Instant lastSubmittedAt,
    List<SubmissionActivityDay> dailyCounts,
    List<TopicMetric> acRateByTopic) {
  public MySubmissionMetrics {
    dailyCounts = List.copyOf(dailyCounts);
    acRateByTopic = acRateByTopic == null ? null : List.copyOf(acRateByTopic);
  }

  public record TopicMetric(
      UUID topicId,
      long submissionCount,
      long acceptedCount,
      Double acRate,
      Instant lastSubmittedAt) {}

  public record TopicRatio(long submissionCount, long acceptedCount, Double acRate) {}
}
