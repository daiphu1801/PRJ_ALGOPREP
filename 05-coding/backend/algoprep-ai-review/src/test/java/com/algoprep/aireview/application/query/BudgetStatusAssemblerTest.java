package com.algoprep.aireview.application.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.algoprep.aireview.application.dto.BudgetStatus;
import com.algoprep.aireview.domain.model.BudgetConfig;
import com.algoprep.aireview.domain.model.FeatureCode;
import com.algoprep.aireview.domain.model.LimitsConfig;
import com.algoprep.aireview.domain.model.ModelPrice;
import com.algoprep.aireview.domain.ports.out.AiBudgetAdminRepository;
import com.algoprep.aireview.domain.ports.out.AiBudgetRepository;
import com.algoprep.aireview.domain.ports.out.AiModelPriceRepository;
import com.algoprep.common.domain.ports.out.ClockPort;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class BudgetStatusAssemblerTest {
  // Period starts 2026-11-01 (UTC+7); "now" is 2026-11-10 12:00 UTC+7 = 05:00Z.
  private static final LocalDate PERIOD = LocalDate.of(2026, 11, 1);
  private static final Instant PERIOD_AT = Instant.parse("2026-10-31T17:00:00Z");
  private static final Instant WINDOW_AT = Instant.parse("2026-11-03T17:00:00Z");
  private final AiBudgetRepository budget = mock(AiBudgetRepository.class);
  private final AiBudgetAdminRepository admin = mock(AiBudgetAdminRepository.class);
  private final ClockPort clock = mock(ClockPort.class);
  private final BudgetStatusAssembler assembler = new BudgetStatusAssembler(budget, admin, clock);

  private void given(long limit, Instant lockedAt, Map<FeatureCode, Long> usage) {
    when(clock.now()).thenReturn(Instant.parse("2026-11-10T05:00:00Z"));
    when(budget.readConfig()).thenReturn(new BudgetConfig(limit, PERIOD, lockedAt, 10, 3, 20));
    when(admin.readLimits()).thenReturn(new LimitsConfig(10, 3, 20, 70));
    when(admin.tokensByFeatureSince(PERIOD_AT)).thenReturn(usage);
  }

  @Test
  void breakdownAlwaysListsAllFourFeaturesAndSumsToTheUsedTotal() {
    given(
        1000, null, Map.of(FeatureCode.SOLUTION_REVIEW, 300L, FeatureCode.PRACTICE_FEEDBACK, 100L));
    BudgetStatus status = assembler.status();
    assertThat(status.featureBreakdown().keySet()).containsExactly(FeatureCode.values());
    assertThat(status.featureBreakdown().get(FeatureCode.MOCK_INTERVIEW)).isZero();
    assertThat(status.usedTokens()).isEqualTo(400);
    assertThat(status.warnPercent()).isEqualTo(70);
    assertThat(status.locked()).isFalse();
  }

  @Test
  void forecastUsesTheSevenDayWindowEndingToday() {
    given(1000, null, Map.of(FeatureCode.SOLUTION_REVIEW, 400L));
    // Window 2026-11-04..2026-11-10 is 7 days; 140 tokens in it means 20 per day; 600 left.
    when(budget.usedTokensSince(WINDOW_AT)).thenReturn(140L);
    assertThat(assembler.status().projectedRunOutDate()).isEqualTo(LocalDate.of(2026, 12, 10));
  }

  @Test
  void anUnconfiguredLimitHasNoForecastAndNeverReadsTheWindow() {
    given(0, null, Map.of(FeatureCode.SOLUTION_REVIEW, 400L));
    BudgetStatus status = assembler.status();
    assertThat(status.projectedRunOutDate()).isNull();
    assertThat(status.locked()).isFalse();
    verify(budget, never()).usedTokensSince(any());
  }

  @Test
  void exhaustedTotalCountsAsLockedEvenWithoutLockedAtAndHasNoForecast() {
    given(400, null, Map.of(FeatureCode.SOLUTION_REVIEW, 400L));
    BudgetStatus status = assembler.status();
    assertThat(status.locked()).isTrue();
    assertThat(status.lockedAt()).isNull();
    assertThat(status.projectedRunOutDate()).isNull();
  }

  @Test
  void aWindowShorterThanSevenDaysIsClampedToThePeriodStart() {
    when(clock.now()).thenReturn(Instant.parse("2026-11-02T05:00:00Z"));
    when(budget.readConfig()).thenReturn(new BudgetConfig(1000, PERIOD, null, 10, 3, 20));
    when(admin.readLimits()).thenReturn(new LimitsConfig(10, 3, 20, 70));
    when(admin.tokensByFeatureSince(PERIOD_AT))
        .thenReturn(Map.of(FeatureCode.SOLUTION_REVIEW, 100L));
    when(budget.usedTokensSince(PERIOD_AT)).thenReturn(100L);
    // 2 days (11-01 and 11-02), 50 per day, 900 left: 18 days after 2026-11-02.
    assertThat(assembler.status().projectedRunOutDate()).isEqualTo(LocalDate.of(2026, 11, 20));
  }

  @Test
  void queryHandlersReturnStoredValues() {
    AiModelPriceRepository prices = mock(AiModelPriceRepository.class);
    ModelPrice row = new ModelPrice(java.util.UUID.randomUUID(), "m", BigDecimal.ONE);
    when(prices.list()).thenReturn(List.of(row));
    given(1000, null, Map.of());
    AdminBudgetQueryHandlers handlers = new AdminBudgetQueryHandlers(assembler, admin, prices);
    assertThat(handlers.status().limitTokens()).isEqualTo(1000);
    assertThat(handlers.read().cooldownSeconds()).isEqualTo(20);
    assertThat(handlers.list()).containsExactly(row);
  }
}
