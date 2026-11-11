package com.algoprep.aireview.infrastructure.redis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.algoprep.aireview.domain.model.CostReservation;
import com.algoprep.aireview.domain.model.FeatureCode;
import com.algoprep.aireview.domain.model.GateOutcome;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

/** Reply parsing and the Redis-down path; the scripts themselves run on real Redis in bootstrap. */
class RedisAiCostGateAdapterTest {
  private static final Instant NOW = Instant.parse("2026-11-09T10:30:00Z");
  private final UUID user = UUID.randomUUID();

  @SuppressWarnings("unchecked")
  private static RedisAiCostGateAdapter adapter(StringRedisTemplate template) {
    ObjectProvider<StringRedisTemplate> provider = mock(ObjectProvider.class);
    when(provider.getObject()).thenReturn(template);
    return new RedisAiCostGateAdapter(provider, () -> NOW);
  }

  @Test
  @SuppressWarnings("unchecked")
  void redisDownBypassesTheGateAndRefundIsSkipped() {
    var template = mock(StringRedisTemplate.class);
    when(template.execute(any(RedisScript.class), anyList(), any(Object[].class)))
        .thenThrow(new IllegalStateException("redis down"));
    var gate = adapter(template);
    CostReservation reservation = gate.reserve(user, FeatureCode.SOLUTION_REVIEW, 10, 20);
    assertThat(reservation.outcome()).isEqualTo(GateOutcome.BYPASSED);
    assertThat(reservation.quotaIncremented()).isFalse();
    gate.refund(
        new CostReservation(
            GateOutcome.GRANTED, user, FeatureCode.SOLUTION_REVIEW, "r", 1, true, 0));
  }

  @Test
  @SuppressWarnings("unchecked")
  void repliesMapToOutcomesAndRetryDelays() {
    var template = mock(StringRedisTemplate.class);
    when(template.execute(any(RedisScript.class), anyList(), any(Object[].class)))
        .thenReturn(List.of(1L, 7L), List.of(2L, 0L), List.of(0L, 0L), null, List.of());
    var gate = adapter(template);
    CostReservation cooldown = gate.reserve(user, FeatureCode.MOCK_INTERVIEW, 3, 20);
    assertThat(cooldown.outcome()).isEqualTo(GateOutcome.COOLDOWN);
    assertThat(cooldown.retryAfterSeconds()).isEqualTo(7);
    CostReservation quota = gate.reserve(user, FeatureCode.MOCK_INTERVIEW, 3, 20);
    assertThat(quota.outcome()).isEqualTo(GateOutcome.QUOTA_EXCEEDED);
    assertThat(quota.retryAfterSeconds()).isEqualTo(1800);
    CostReservation granted = gate.reserve(user, FeatureCode.MOCK_INTERVIEW, 3, 20);
    assertThat(granted.granted()).isTrue();
    assertThat(granted.epochHour()).isEqualTo(NOW.getEpochSecond() / 3600);
    assertThat(granted.requestId()).isNotBlank();
    assertThat(gate.reserve(user, FeatureCode.MOCK_INTERVIEW, 3, 20).outcome())
        .isEqualTo(GateOutcome.BYPASSED);
    assertThat(gate.reserve(user, FeatureCode.MOCK_INTERVIEW, 3, 20).outcome())
        .isEqualTo(GateOutcome.BYPASSED);
  }

  @Test
  @SuppressWarnings("unchecked")
  void onlyAGrantedReservationIsRefunded() {
    var template = mock(StringRedisTemplate.class);
    var gate = adapter(template);
    for (GateOutcome outcome :
        List.of(GateOutcome.COOLDOWN, GateOutcome.QUOTA_EXCEEDED, GateOutcome.BYPASSED)) {
      gate.refund(
          new CostReservation(outcome, user, FeatureCode.SOLUTION_REVIEW, "r", 1, false, 0));
    }
    verify(template, never()).execute(any(RedisScript.class), anyList(), any(Object[].class));
  }

  @Test
  void featuresWithoutAnHourlyQuotaAreRefused() {
    var gate = adapter(mock(StringRedisTemplate.class));
    assertThatThrownBy(() -> gate.reserve(user, FeatureCode.GENERATE_TESTCASE, 1, 1))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
