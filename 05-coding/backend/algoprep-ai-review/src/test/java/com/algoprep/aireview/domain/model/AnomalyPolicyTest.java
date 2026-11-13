package com.algoprep.aireview.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AnomalyPolicyTest {
  private static final BigDecimal FIVE = new BigDecimal("5");

  /** One heavy user and {@code others} users with one call each. */
  private static Map<UUID, Integer> population(UUID heavy, int heavyCalls, int others) {
    Map<UUID, Integer> counts = new HashMap<>();
    counts.put(heavy, heavyCalls);
    for (int i = 0; i < others; i++) {
      counts.put(UUID.randomUUID(), 1);
    }
    return counts;
  }

  @Test
  void nobodyActiveFlagsNobody() {
    assertThat(AnomalyPolicy.flagged(Map.of(), FIVE)).isEmpty();
  }

  @Test
  void aRatioExactlyAtTheThresholdIsFlagged() {
    UUID heavy = UUID.randomUUID();
    // 25 + 5 x 1 = 30 calls over 6 users: average 5.00, ratio 25 / 5.00 = 5.00
    var flagged = AnomalyPolicy.flagged(population(heavy, 25, 5), FIVE);
    assertThat(flagged).hasSize(1);
    assertThat(flagged.get(0).userId()).isEqualTo(heavy);
    assertThat(flagged.get(0).callCount()).isEqualTo(25);
    assertThat(flagged.get(0).baselineAvg()).isEqualByComparingTo("5.00");
    assertThat(flagged.get(0).ratio()).isEqualByComparingTo("5.00");
  }

  @Test
  void aRatioJustBelowTheThresholdIsNotFlagged() {
    // 24 + 5 x 1 = 29 / 6 = 4.83; ratio 24 / 4.83 = 4.97
    assertThat(AnomalyPolicy.flagged(population(UUID.randomUUID(), 24, 5), FIVE)).isEmpty();
  }

  @Test
  void theAverageAndRatioAreStoredWithTwoDecimals() {
    UUID heavy = UUID.randomUUID();
    // 10 + 10 x 1 = 20 / 11 = 1.82; ratio 10 / 1.82 = 5.49
    var flagged = AnomalyPolicy.flagged(population(heavy, 10, 10), FIVE);
    assertThat(flagged).hasSize(1);
    assertThat(flagged.get(0).baselineAvg()).isEqualByComparingTo("1.82");
    assertThat(flagged.get(0).ratio()).isEqualByComparingTo("5.49");
  }

  @Test
  void aLoneUserIsNeverFlaggedAtFiveTimes() {
    assertThat(AnomalyPolicy.flagged(Map.of(UUID.randomUUID(), 50), FIVE)).isEmpty();
    assertThat(AnomalyPolicy.flagged(Map.of(UUID.randomUUID(), 50), BigDecimal.ONE)).hasSize(1);
  }

  @Test
  void theRatioIsCappedToWhatTheColumnHolds() {
    var flagged = AnomalyPolicy.flagged(population(UUID.randomUUID(), 50000, 2000), FIVE);
    assertThat(flagged).hasSize(1);
    assertThat(flagged.get(0).ratio()).isEqualByComparingTo("999.99");
  }
}
