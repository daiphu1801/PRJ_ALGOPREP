package com.algoprep.bootstrap.identity;

import com.algoprep.identity.application.ports.out.JudgeStatsPort;
import com.algoprep.identity.domain.model.AdminSubmissionBlocks.DayColumn;
import com.algoprep.identity.domain.model.AdminSubmissionBlocks.Days;
import com.algoprep.identity.domain.model.AdminSubmissionBlocks.LanguageSeries;
import com.algoprep.identity.domain.model.AdminSubmissionBlocks.Languages;
import com.algoprep.identity.domain.model.AdminSubmissionBlocks.LevelCount;
import com.algoprep.identity.domain.model.AdminSubmissionBlocks.LevelScope;
import com.algoprep.identity.domain.model.AdminSubmissionBlocks.MonthPoint;
import com.algoprep.identity.domain.model.AdminSubmissionBlocks.Months;
import com.algoprep.identity.domain.model.AdminSubmissionBlocks.ProblemCount;
import com.algoprep.identity.domain.model.AdminSubmissionBlocks.VerdictSlice;
import com.algoprep.identity.domain.model.AdminSubmissionBlocks.Verdicts;
import com.algoprep.identity.domain.model.StatSummary;
import com.algoprep.judge.application.ports.in.GetAdminDifficultyBreakdownUseCase;
import com.algoprep.judge.application.ports.in.GetAdminSubmissionsByDayUseCase;
import com.algoprep.judge.application.ports.in.GetAdminSubmissionsByLanguageUseCase;
import com.algoprep.judge.application.ports.in.GetAdminSubmissionsByMonthUseCase;
import com.algoprep.judge.application.ports.in.GetAdminSubmissionsSummaryUseCase;
import com.algoprep.judge.application.ports.in.GetAdminTopProblemsUseCase;
import com.algoprep.judge.application.ports.in.GetAdminVerdictDistributionUseCase;
import com.algoprep.judge.domain.model.AdminSubmissionStats;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Type translation only: identity asks for the submission statistics of the admin overview, the
 * judge module answers through its seven inbound ports (DD api F1 section 0.8.5, DD api F4 section
 * 26.2). Each read runs on a virtual thread with a 3 second deadline and a failure is one {@link
 * IllegalStateException}, never an invented number.
 */
@Component
public final class JudgeStatsBridge implements JudgeStatsPort {
  private final GetAdminSubmissionsSummaryUseCase summary;
  private final GetAdminSubmissionsByLanguageUseCase languages;
  private final GetAdminVerdictDistributionUseCase verdicts;
  private final GetAdminDifficultyBreakdownUseCase difficulty;
  private final GetAdminSubmissionsByDayUseCase days;
  private final GetAdminSubmissionsByMonthUseCase months;
  private final GetAdminTopProblemsUseCase top;
  private final Duration limit;

  @Autowired
  public JudgeStatsBridge(Reads reads) {
    this(reads, Duration.ofSeconds(3));
  }

  JudgeStatsBridge(Reads reads, Duration limit) {
    this.summary = reads.summary();
    this.languages = reads.languages();
    this.verdicts = reads.verdicts();
    this.difficulty = reads.difficulty();
    this.days = reads.days();
    this.months = reads.months();
    this.top = reads.top();
    this.limit = limit;
  }

  /** The seven inbound ports of the judge module, grouped to keep the constructor short. */
  @Component
  public record Reads(
      GetAdminSubmissionsSummaryUseCase summary,
      GetAdminSubmissionsByLanguageUseCase languages,
      GetAdminVerdictDistributionUseCase verdicts,
      GetAdminDifficultyBreakdownUseCase difficulty,
      GetAdminSubmissionsByDayUseCase days,
      GetAdminSubmissionsByMonthUseCase months,
      GetAdminTopProblemsUseCase top) {}

  @Override
  public StatSummary summary() {
    return run(
        () -> {
          var s = summary.summary();
          return new StatSummary(
              s.value(),
              s.deltaPercent() == null ? null : s.deltaPercent().doubleValue(),
              s.deltaDirection(),
              s.sparklineSeries());
        });
  }

  @Override
  public Languages byLanguage() {
    return run(
        () -> {
          var l = languages.byLanguage();
          return new Languages(
              l.pointLabels(),
              l.series().stream()
                  .map(s -> new LanguageSeries(s.languageCode(), s.values()))
                  .toList());
        });
  }

  @Override
  public Verdicts verdicts() {
    return run(
        () ->
            new Verdicts(
                verdicts.verdicts().slices().stream()
                    .map(s -> new VerdictSlice(s.verdict(), s.count(), s.percent()))
                    .toList()));
  }

  @Override
  public List<LevelCount> byDifficulty(List<LevelScope> levels) {
    var scope =
        levels.stream()
            .map(l -> new AdminSubmissionStats.LevelProblems(l.levelId(), l.problemIds()))
            .toList();
    return run(
        () ->
            difficulty.byDifficulty(scope).groups().stream()
                .map(g -> new LevelCount(g.levelId(), g.submissions(), g.accepted()))
                .toList());
  }

  @Override
  public Days byDay() {
    return run(
        () -> {
          var d = days.byDay();
          return new Days(
              d.columns().stream().map(c -> new DayColumn(c.day().toString(), c.count())).toList(),
              d.weekTotal(),
              d.dailyAverage());
        });
  }

  @Override
  public Months byMonth() {
    return run(
        () -> {
          var m = months.byMonth();
          return new Months(
              m.points().stream()
                  .map(p -> new MonthPoint(p.month().toString(), p.count()))
                  .toList(),
              m.monthTotal());
        });
  }

  @Override
  public List<ProblemCount> topProblems(Set<UUID> eligibleProblemIds) {
    return run(
        () ->
            top.topProblems(eligibleProblemIds).items().stream()
                .map(p -> new ProblemCount(p.problemId(), p.submissionCount()))
                .toList());
  }

  private <T> T run(Callable<T> work) {
    return BridgeDeadline.call("judge-stats-bridge", limit, work);
  }
}
