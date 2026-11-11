package com.algoprep.aireview.application.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.algoprep.aireview.application.dto.BudgetStatus;
import com.algoprep.aireview.application.dto.TopProblemItem;
import com.algoprep.aireview.application.dto.TopUserItem;
import com.algoprep.aireview.application.dto.UsageAlertItem;
import com.algoprep.aireview.application.dto.UsageDaily;
import com.algoprep.aireview.application.ports.out.ProblemBankStatsPort;
import com.algoprep.aireview.application.ports.out.UserDirectoryPort;
import com.algoprep.aireview.domain.model.AnomalyAlert;
import com.algoprep.aireview.domain.model.DailyFeatureTokens;
import com.algoprep.aireview.domain.model.FeatureCode;
import com.algoprep.aireview.domain.model.ProblemSummary;
import com.algoprep.aireview.domain.model.ProblemUsage;
import com.algoprep.aireview.domain.model.UserUsage;
import com.algoprep.aireview.domain.ports.out.AiUsageRepository;
import com.algoprep.common.domain.ports.out.ClockPort;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AdminUsageQueryHandlersTest {
  // 2026-11-10 12:00 UTC+7 = 05:00Z.
  private static final Instant NOW = Instant.parse("2026-11-10T05:00:00Z");
  private final AiUsageRepository usage = mock(AiUsageRepository.class);
  private final BudgetStatusAssembler budget = mock(BudgetStatusAssembler.class);
  private final UserDirectoryPort users = mock(UserDirectoryPort.class);
  private final ProblemBankStatsPort problems = mock(ProblemBankStatsPort.class);
  private final ClockPort clock = mock(ClockPort.class);
  private final AdminUsageQueryHandlers handlers =
      new AdminUsageQueryHandlers(usage, budget, users, problems, clock);
  private final UUID u1 = UUID.randomUUID();
  private final UUID p1 = UUID.randomUUID();

  @BeforeEach
  void fixedClock() {
    when(clock.now()).thenReturn(NOW);
  }

  private static BudgetStatus status(long limit, long used, boolean locked, int warn) {
    return new BudgetStatus(
        limit,
        used,
        LocalDate.of(2026, 11, 1),
        null,
        locked,
        LocalDate.of(2026, 12, 1),
        new EnumMap<>(FeatureCode.class),
        warn);
  }

  private static AnomalyAlert alert(UUID user) {
    return new AnomalyAlert(
        UUID.randomUUID(),
        user,
        NOW.minusSeconds(86400),
        NOW,
        30,
        new BigDecimal("5.00"),
        new BigDecimal("6.00"),
        NOW);
  }

  @Test
  void dailyIsADenseSeriesOldestFirstEndingTodayWithZerosFilledIn() {
    when(usage.dailyTokens(
            Instant.parse("2026-11-03T17:00:00Z"), Instant.parse("2026-11-10T17:00:00Z")))
        .thenReturn(
            List.of(
                new DailyFeatureTokens(LocalDate.of(2026, 11, 10), FeatureCode.SOLUTION_REVIEW, 70),
                new DailyFeatureTokens(LocalDate.of(2026, 11, 4), FeatureCode.MOCK_INTERVIEW, 5)));
    UsageDaily daily = handlers.daily(7);
    assertThat(daily.rangeDays()).isEqualTo(7);
    assertThat(daily.points()).hasSize(7);
    assertThat(daily.points().get(0).date()).isEqualTo(LocalDate.of(2026, 11, 4));
    assertThat(daily.points().get(6).date()).isEqualTo(LocalDate.of(2026, 11, 10));
    assertThat(daily.points().get(6).tokensByFeature().get(FeatureCode.SOLUTION_REVIEW))
        .isEqualTo(70);
    assertThat(daily.points().get(0).tokensByFeature().get(FeatureCode.MOCK_INTERVIEW))
        .isEqualTo(5);
    assertThat(daily.points().get(3).tokensByFeature().values()).containsOnly(0L);
    assertThat(daily.points().get(3).tokensByFeature().keySet())
        .containsExactly(FeatureCode.values());
  }

  @Test
  void alertsListTheBudgetCardFirstWhenUsageReachesWarnPercent() {
    when(usage.openAlerts()).thenReturn(List.of(alert(u1)));
    when(users.getDisplayNames(List.of(u1))).thenReturn(Map.of(u1, "An"));
    when(budget.status()).thenReturn(status(1000, 700, false, 70));
    List<UsageAlertItem> items = handlers.list();
    assertThat(items).hasSize(2);
    assertThat(items.get(0).alertType()).isEqualTo(UsageAlertItem.Type.BUDGET);
    assertThat(items.get(0).usedPercent()).isEqualByComparingTo("70.0");
    assertThat(items.get(0).alertId()).isNull();
    assertThat(items.get(1).alertType()).isEqualTo(UsageAlertItem.Type.ANOMALY);
    assertThat(items.get(1).displayName()).isEqualTo("An");
    assertThat(items.get(1).status()).isEqualTo("OPEN");
  }

  @Test
  void noBudgetCardBelowTheWarnPercentOrWithoutALimit() {
    when(usage.openAlerts()).thenReturn(List.of());
    when(budget.status()).thenReturn(status(1000, 699, false, 70));
    assertThat(handlers.list()).isEmpty();
    when(budget.status()).thenReturn(status(0, 500, true, 70));
    assertThat(handlers.list()).isEmpty();
  }

  @Test
  void aLockedBudgetShowsTheCardEvenUnderTheWarnPercent() {
    when(usage.openAlerts()).thenReturn(List.of());
    when(budget.status()).thenReturn(status(1000, 10, true, 70));
    assertThat(handlers.list()).hasSize(1);
  }

  @Test
  void aFailingDirectoryLeavesTheNameNullInsteadOfFailing() {
    when(usage.openAlerts()).thenReturn(List.of(alert(u1)));
    when(users.getDisplayNames(any())).thenThrow(new IllegalStateException("down"));
    when(budget.status()).thenReturn(status(0, 0, false, 70));
    assertThat(handlers.list().get(0).displayName()).isNull();
  }

  @Test
  void topUsersLookBack30DaysAndAttachNamesAndCost() {
    Instant since = Instant.parse("2026-10-11T05:00:00Z");
    when(usage.topUsers(since, 5)).thenReturn(List.of(new UserUsage(u1, 900, 3, null)));
    when(users.getDisplayNames(List.of(u1))).thenReturn(Map.of(u1, "An"));
    List<TopUserItem> top = handlers.topUsers(5);
    assertThat(top).hasSize(1);
    assertThat(top.get(0).displayName()).isEqualTo("An");
    assertThat(top.get(0).costUsd()).isNull();
    assertThat(top.get(0).tokens()).isEqualTo(900);
  }

  @Test
  void topUsersWithADownDirectoryKeepTheRowsWithNullNames() {
    when(usage.topUsers(any(), org.mockito.ArgumentMatchers.anyInt()))
        .thenReturn(List.of(new UserUsage(u1, 900, 3, BigDecimal.ONE)));
    when(users.getDisplayNames(any())).thenThrow(new IllegalStateException("down"));
    assertThat(handlers.topUsers(5).get(0).displayName()).isNull();
  }

  @Test
  void emptyListsDoNotCallTheNeighbourModules() {
    when(usage.topUsers(any(), org.mockito.ArgumentMatchers.anyInt())).thenReturn(List.of());
    when(usage.topProblems(any(), org.mockito.ArgumentMatchers.anyInt())).thenReturn(List.of());
    assertThat(handlers.topUsers(5)).isEmpty();
    assertThat(handlers.topProblems(5)).isEmpty();
    verify(users, never()).getDisplayNames(any());
    verify(problems, never()).problemSummaries(any());
  }

  @Test
  void topProblemsRoundTheAverageAndAttachTitles() {
    when(usage.topProblems(Instant.parse("2026-10-11T05:00:00Z"), 5))
        .thenReturn(List.of(new ProblemUsage(p1, 1000, 3)));
    when(problems.problemSummaries(Set.of(p1)))
        .thenReturn(List.of(new ProblemSummary(p1, "Two Sum", "EASY", "De")));
    List<TopProblemItem> top = handlers.topProblems(5);
    assertThat(top.get(0).avgTokensPerCall()).isEqualTo(333);
    assertThat(top.get(0).problemTitle()).isEqualTo("Two Sum");
    assertThat(top.get(0).levelDisplayName()).isEqualTo("De");
  }

  @Test
  void topProblemsKeepRowsWithNullTitlesWhenProblemBankFails() {
    when(usage.topProblems(any(), org.mockito.ArgumentMatchers.anyInt()))
        .thenReturn(List.of(new ProblemUsage(p1, 5, 2)));
    when(problems.problemSummaries(any())).thenThrow(new IllegalStateException("down"));
    TopProblemItem item = handlers.topProblems(5).get(0);
    assertThat(item.problemTitle()).isNull();
    assertThat(item.levelCode()).isNull();
    assertThat(item.avgTokensPerCall()).isEqualTo(3);
  }
}
