package com.algoprep.aireview.application.query;

import com.algoprep.aireview.application.dto.BudgetStatus;
import com.algoprep.aireview.application.dto.TopProblemItem;
import com.algoprep.aireview.application.dto.TopUserItem;
import com.algoprep.aireview.application.dto.UsageAlertItem;
import com.algoprep.aireview.application.dto.UsageDaily;
import com.algoprep.aireview.application.ports.in.GetAiUsageDailyUseCase;
import com.algoprep.aireview.application.ports.in.ListAiUsageAlertsUseCase;
import com.algoprep.aireview.application.ports.in.ListTopAiProblemsUseCase;
import com.algoprep.aireview.application.ports.in.ListTopAiUsersUseCase;
import com.algoprep.aireview.application.ports.out.ProblemBankStatsPort;
import com.algoprep.aireview.application.ports.out.UserDirectoryPort;
import com.algoprep.aireview.domain.model.AnomalyAlert;
import com.algoprep.aireview.domain.model.BudgetPolicy;
import com.algoprep.aireview.domain.model.DailyFeatureTokens;
import com.algoprep.aireview.domain.model.FeatureCode;
import com.algoprep.aireview.domain.model.ProblemSummary;
import com.algoprep.aireview.domain.model.ProblemUsage;
import com.algoprep.aireview.domain.model.UserUsage;
import com.algoprep.aireview.domain.ports.out.AiUsageRepository;
import com.algoprep.common.domain.ports.out.ClockPort;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Endpoints 27, 28, 30 and 31 (ADM0302): stored data only, so they work while the LLM is down.
 * Names from identity and problem-bank are optional; their failure leaves the field null.
 */
@Service
@Transactional(readOnly = true)
class AdminUsageQueryHandlers
    implements GetAiUsageDailyUseCase,
        ListAiUsageAlertsUseCase,
        ListTopAiUsersUseCase,
        ListTopAiProblemsUseCase {
  /** Top lists look at a fixed 30 day window (DD F5 sections 30.2 and 31.2). */
  static final int TOP_WINDOW_DAYS = 30;

  private final AiUsageRepository usage;
  private final BudgetStatusAssembler budget;
  private final UserDirectoryPort users;
  private final ProblemBankStatsPort problems;
  private final ClockPort clock;

  AdminUsageQueryHandlers(
      AiUsageRepository usage,
      BudgetStatusAssembler budget,
      UserDirectoryPort users,
      ProblemBankStatsPort problems,
      ClockPort clock) {
    this.usage = usage;
    this.budget = budget;
    this.users = users;
    this.problems = problems;
    this.clock = clock;
  }

  @Override
  public UsageDaily daily(int rangeDays) {
    LocalDate today = BudgetPolicy.dayOf(clock.now());
    LocalDate first = today.minusDays(rangeDays - 1L);
    Map<LocalDate, Map<FeatureCode, Long>> byDay = new HashMap<>();
    for (DailyFeatureTokens row :
        usage.dailyTokens(
            BudgetPolicy.periodStartAt(first), BudgetPolicy.periodStartAt(today.plusDays(1)))) {
      byDay
          .computeIfAbsent(row.day(), d -> new EnumMap<>(FeatureCode.class))
          .merge(row.feature(), row.tokens(), Long::sum);
    }
    List<UsageDaily.Point> points = new ArrayList<>(rangeDays);
    for (int i = 0; i < rangeDays; i++) {
      LocalDate day = first.plusDays(i);
      Map<FeatureCode, Long> raw = byDay.getOrDefault(day, Map.of());
      Map<FeatureCode, Long> filled = new EnumMap<>(FeatureCode.class);
      for (FeatureCode code : FeatureCode.values()) {
        filled.put(code, raw.getOrDefault(code, 0L));
      }
      points.add(new UsageDaily.Point(day, filled));
    }
    return new UsageDaily(rangeDays, points);
  }

  @Override
  public List<UsageAlertItem> list() {
    List<AnomalyAlert> open = usage.openAlerts();
    Map<UUID, String> names = names(open.stream().map(AnomalyAlert::userId).toList());
    List<UsageAlertItem> items = new ArrayList<>();
    BudgetStatus status = budget.status();
    if (status.limitTokens() > 0
        && (status.locked()
            || status.usedTokens() * 100 >= (long) status.warnPercent() * status.limitTokens())) {
      items.add(
          new UsageAlertItem(
              UsageAlertItem.Type.BUDGET,
              null,
              null,
              null,
              null,
              null,
              null,
              null,
              null,
              null,
              status.projectedRunOutDate(),
              BigDecimal.valueOf(status.usedTokens() * 100L)
                  .divide(BigDecimal.valueOf(status.limitTokens()), 1, RoundingMode.HALF_UP)));
    }
    for (AnomalyAlert alert : open) {
      items.add(
          new UsageAlertItem(
              UsageAlertItem.Type.ANOMALY,
              alert.id(),
              "OPEN",
              alert.callCount(),
              alert.baselineAvg(),
              alert.ratio(),
              alert.windowStart(),
              alert.windowEnd(),
              alert.userId(),
              names.get(alert.userId()),
              null,
              null));
    }
    return items;
  }

  @Override
  public List<TopUserItem> topUsers(int limit) {
    List<UserUsage> rows = usage.topUsers(since(), limit);
    Map<UUID, String> names = names(rows.stream().map(UserUsage::userId).toList());
    return rows.stream()
        .map(
            r ->
                new TopUserItem(
                    r.userId(), names.get(r.userId()), r.tokens(), r.calls(), r.costUsd()))
        .toList();
  }

  @Override
  public List<TopProblemItem> topProblems(int limit) {
    List<ProblemUsage> rows = usage.topProblems(since(), limit);
    Map<UUID, ProblemSummary> summaries = summaries(rows);
    return rows.stream()
        .map(
            r -> {
              ProblemSummary s = summaries.get(r.problemId());
              return new TopProblemItem(
                  r.problemId(),
                  s == null ? null : s.title(),
                  s == null ? null : s.levelCode(),
                  s == null ? null : s.levelDisplayName(),
                  r.tokens(),
                  r.calls(),
                  (int) Math.round((double) r.tokens() / r.calls()));
            })
        .toList();
  }

  private Instant since() {
    return clock.now().minus(TOP_WINDOW_DAYS, ChronoUnit.DAYS);
  }

  private Map<UUID, String> names(List<UUID> ids) {
    if (ids.isEmpty()) {
      return Map.of();
    }
    try {
      return users.getDisplayNames(ids);
    } catch (RuntimeException unavailable) {
      return Map.of();
    }
  }

  private Map<UUID, ProblemSummary> summaries(List<ProblemUsage> rows) {
    if (rows.isEmpty()) {
      return Map.of();
    }
    try {
      Set<UUID> ids = rows.stream().map(ProblemUsage::problemId).collect(Collectors.toSet());
      return problems.problemSummaries(ids).stream()
          .collect(Collectors.toMap(ProblemSummary::problemId, p -> p, (a, b) -> a));
    } catch (RuntimeException unavailable) {
      return Map.of();
    }
  }
}
