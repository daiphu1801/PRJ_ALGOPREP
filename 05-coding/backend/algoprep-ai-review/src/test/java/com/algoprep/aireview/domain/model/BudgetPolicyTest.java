package com.algoprep.aireview.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class BudgetPolicyTest {
  private static BudgetConfig config(long limit, Instant lockedAt) {
    return new BudgetConfig(limit, LocalDate.of(2026, 11, 1), lockedAt, 10, 3, 20);
  }

  @Test
  void aLimitOfZeroMeansNotConfiguredAndNeverExhausts() {
    assertThat(BudgetPolicy.exhausted(0, 0)).isFalse();
    assertThat(BudgetPolicy.exhausted(0, 1_000_000)).isFalse();
  }

  @Test
  void exhaustedStartsExactlyAtTheLimit() {
    assertThat(BudgetPolicy.exhausted(1000, 999)).isFalse();
    assertThat(BudgetPolicy.exhausted(1000, 1000)).isTrue();
    assertThat(BudgetPolicy.exhausted(1000, 1500)).isTrue();
  }

  @Test
  void lockedWhenLockedAtIsSetOrWhenTheTotalAlreadyReachedTheLimit() {
    assertThat(BudgetPolicy.locked(config(1000, null), 10)).isFalse();
    assertThat(BudgetPolicy.locked(config(1000, Instant.now()), 10)).isTrue();
    assertThat(BudgetPolicy.locked(config(1000, null), 1000)).isTrue();
    assertThat(BudgetPolicy.locked(config(0, null), 5)).isFalse();
  }

  @Test
  void thePeriodStartsAtMidnightUtcPlusSeven() {
    assertThat(BudgetPolicy.periodStartAt(LocalDate.of(2026, 11, 1)))
        .isEqualTo(Instant.parse("2026-10-31T17:00:00Z"));
  }

  @Test
  void smallValueObjectsGuardTheirInputs() {
    assertThatThrownBy(() -> new LlmUsage(-1, 0)).isInstanceOf(IllegalArgumentException.class);
    assertThat(new LlmUsage(3, 4).completionTokens()).isEqualTo(4);
    assertThat(FeatureCode.SOLUTION_REVIEW.hasHourlyQuota()).isTrue();
    assertThat(FeatureCode.MOCK_INTERVIEW.hasHourlyQuota()).isTrue();
    assertThat(FeatureCode.GENERATE_TESTCASE.hasHourlyQuota()).isFalse();
    assertThat(FeatureCode.PRACTICE_FEEDBACK.hasHourlyQuota()).isTrue();
    UUID id = UUID.randomUUID();
    assertThatThrownBy(
            () -> new TokenUsageRecord(id, id, FeatureCode.PRACTICE_FEEDBACK, null, id, 1, 1, null))
        .isInstanceOf(NullPointerException.class);
    CostReservation granted =
        new CostReservation(GateOutcome.GRANTED, id, FeatureCode.SOLUTION_REVIEW, "r", 1, true, 0);
    assertThat(granted.granted()).isTrue();
    assertThat(granted.rejected()).isFalse();
    assertThat(
            new CostReservation(
                    GateOutcome.COOLDOWN, id, FeatureCode.SOLUTION_REVIEW, "r", 1, false, 5)
                .rejected())
        .isTrue();
    assertThat(
            new CostReservation(
                    GateOutcome.BYPASSED, id, FeatureCode.SOLUTION_REVIEW, "r", 1, false, 0)
                .rejected())
        .isFalse();
  }
}
