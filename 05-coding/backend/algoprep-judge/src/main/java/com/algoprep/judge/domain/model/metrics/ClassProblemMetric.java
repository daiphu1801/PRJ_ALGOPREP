package com.algoprep.judge.domain.model;

/** Only the contracted aggregate leaves the repository; no learner or source data. */
public record ClassProblemMetric(long submissionCount, Double acRatePercent) {
  public static ClassProblemMetric from(long total, long eligible, long accepted) {
    return new ClassProblemMetric(total, eligible == 0 ? null : 100.0 * accepted / eligible);
  }
}
