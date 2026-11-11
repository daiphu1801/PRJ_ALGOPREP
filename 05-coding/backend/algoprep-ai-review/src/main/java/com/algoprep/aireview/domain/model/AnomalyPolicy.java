package com.algoprep.aireview.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Pure rule of JOB-AI-05: a user at or above {@code threshold} times the average is flagged. */
public final class AnomalyPolicy {
  /** The ratio column is NUMERIC(5,2). */
  static final BigDecimal MAX_RATIO = new BigDecimal("999.99");

  private AnomalyPolicy() {}

  /** One candidate: the call count of a user with the average and ratio as they are stored. */
  public record Candidate(UUID userId, int callCount, BigDecimal baselineAvg, BigDecimal ratio) {}

  /**
   * The average is total calls over users with at least one call, rounded to two decimals (all
   * roles count, DEC-2026-1028-qa-f5-anomaly-baseline-admin). Empty when there is nobody or the
   * average is zero.
   */
  public static List<Candidate> flagged(Map<UUID, Integer> callCounts, BigDecimal threshold) {
    if (callCounts.isEmpty()) {
      return List.of();
    }
    long total = callCounts.values().stream().mapToLong(Integer::longValue).sum();
    BigDecimal baseline =
        BigDecimal.valueOf(total)
            .divide(BigDecimal.valueOf(callCounts.size()), 2, RoundingMode.HALF_UP);
    if (baseline.signum() <= 0) {
      return List.of();
    }
    List<Candidate> result = new ArrayList<>();
    for (Map.Entry<UUID, Integer> entry : callCounts.entrySet()) {
      BigDecimal ratio =
          BigDecimal.valueOf(entry.getValue()).divide(baseline, 6, RoundingMode.HALF_UP);
      if (ratio.compareTo(threshold) >= 0) {
        result.add(
            new Candidate(
                entry.getKey(),
                entry.getValue(),
                baseline,
                ratio.setScale(2, RoundingMode.HALF_UP).min(MAX_RATIO)));
      }
    }
    return result;
  }
}
