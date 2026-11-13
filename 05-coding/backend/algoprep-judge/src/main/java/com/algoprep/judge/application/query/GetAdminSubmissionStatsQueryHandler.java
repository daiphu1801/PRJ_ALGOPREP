package com.algoprep.judge.application.query;

import com.algoprep.judge.application.ports.in.GetAdminDifficultyBreakdownUseCase;
import com.algoprep.judge.application.ports.in.GetAdminSubmissionsByDayUseCase;
import com.algoprep.judge.application.ports.in.GetAdminSubmissionsByLanguageUseCase;
import com.algoprep.judge.application.ports.in.GetAdminSubmissionsByMonthUseCase;
import com.algoprep.judge.application.ports.in.GetAdminSubmissionsSummaryUseCase;
import com.algoprep.judge.application.ports.in.GetAdminTopProblemsUseCase;
import com.algoprep.judge.application.ports.in.GetAdminVerdictDistributionUseCase;
import com.algoprep.judge.domain.metrics.AdminSubmissionStatsCalculator;
import com.algoprep.judge.domain.model.AdminSubmissionStats;
import com.algoprep.judge.domain.model.AdminSubmissionStats.LevelProblems;
import com.algoprep.judge.domain.ports.out.AdminSubmissionStatsRepository;
import java.time.YearMonth;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
class GetAdminSubmissionStatsQueryHandler
    implements GetAdminSubmissionsSummaryUseCase,
        GetAdminSubmissionsByLanguageUseCase,
        GetAdminVerdictDistributionUseCase,
        GetAdminDifficultyBreakdownUseCase,
        GetAdminSubmissionsByDayUseCase,
        GetAdminSubmissionsByMonthUseCase,
        GetAdminTopProblemsUseCase {
  private final AdminSubmissionStatsRepository repository;

  GetAdminSubmissionStatsQueryHandler(AdminSubmissionStatsRepository repository) {
    this.repository = repository;
  }

  @Override
  public AdminSubmissionStats.Summary summary() {
    return read(
        () -> {
          var now = repository.now();
          var today = AdminSubmissionStatsCalculator.today(now);
          var daily =
              repository.daily(AdminSubmissionStatsCalculator.dayStart(today.minusDays(6)), now);
          return AdminSubmissionStatsCalculator.summary(
              repository.totals(now), AdminSubmissionStatsCalculator.days(daily, today, 7));
        });
  }

  @Override
  public AdminSubmissionStats.Languages byLanguage() {
    return read(
        () -> {
          var now = repository.now();
          var today = AdminSubmissionStatsCalculator.today(now);
          return AdminSubmissionStatsCalculator.languages(
              repository.languages(
                  AdminSubmissionStatsCalculator.dayStart(today.minusDays(19)), now),
              today);
        });
  }

  @Override
  public AdminSubmissionStats.Verdicts verdicts() {
    return read(
        () -> AdminSubmissionStatsCalculator.verdicts(repository.verdicts(repository.now())));
  }

  @Override
  public AdminSubmissionStats.Difficulty byDifficulty(List<LevelProblems> levels) {
    var scope = List.copyOf(levels);
    var ids = AdminSubmissionStatsCalculator.problemIds(scope);
    return read(
        () ->
            AdminSubmissionStatsCalculator.difficulty(
                scope, repository.problems(ids, repository.now())));
  }

  @Override
  public AdminSubmissionStats.Days byDay() {
    return read(
        () -> {
          var now = repository.now();
          var today = AdminSubmissionStatsCalculator.today(now);
          var rows =
              repository.daily(AdminSubmissionStatsCalculator.dayStart(today.minusDays(6)), now);
          return AdminSubmissionStatsCalculator.daily(
              AdminSubmissionStatsCalculator.days(rows, today, 7));
        });
  }

  @Override
  public AdminSubmissionStats.Months byMonth() {
    return read(
        () -> {
          var now = repository.now();
          var today = AdminSubmissionStatsCalculator.today(now);
          var start = YearMonth.from(today).minusMonths(5).atDay(1);
          return AdminSubmissionStatsCalculator.months(
              repository.months(AdminSubmissionStatsCalculator.dayStart(start), now), today);
        });
  }

  @Override
  public AdminSubmissionStats.Top topProblems(Set<UUID> eligibleProblemIds) {
    var scope = Set.copyOf(eligibleProblemIds);
    return read(
        () -> AdminSubmissionStatsCalculator.top(repository.problems(scope, repository.now())));
  }

  private <T> T read(Supplier<T> operation) {
    try {
      return operation.get();
    } catch (RuntimeException unavailable) {
      throw new IllegalStateException("Admin submission statistics are temporarily unavailable");
    }
  }
}
