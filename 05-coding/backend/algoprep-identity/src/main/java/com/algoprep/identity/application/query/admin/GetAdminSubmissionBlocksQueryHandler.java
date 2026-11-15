package com.algoprep.identity.application.query;

import com.algoprep.common.exception.ServiceUnavailableException;
import com.algoprep.identity.application.ports.in.GetAdminSubmissionBlocksUseCase;
import com.algoprep.identity.application.ports.out.JudgeStatsPort;
import com.algoprep.identity.application.ports.out.ProblemBankStatsPort;
import com.algoprep.identity.domain.exception.IdentityErrorCodes;
import com.algoprep.identity.domain.model.AdminSubmissionBlocks.Days;
import com.algoprep.identity.domain.model.AdminSubmissionBlocks.Difficulty;
import com.algoprep.identity.domain.model.AdminSubmissionBlocks.DifficultyGroup;
import com.algoprep.identity.domain.model.AdminSubmissionBlocks.Languages;
import com.algoprep.identity.domain.model.AdminSubmissionBlocks.LevelCount;
import com.algoprep.identity.domain.model.AdminSubmissionBlocks.LevelRef;
import com.algoprep.identity.domain.model.AdminSubmissionBlocks.LevelScope;
import com.algoprep.identity.domain.model.AdminSubmissionBlocks.Months;
import com.algoprep.identity.domain.model.AdminSubmissionBlocks.ProblemCount;
import com.algoprep.identity.domain.model.AdminSubmissionBlocks.PublishedProblem;
import com.algoprep.identity.domain.model.AdminSubmissionBlocks.TopProblem;
import com.algoprep.identity.domain.model.AdminSubmissionBlocks.TopProblems;
import com.algoprep.identity.domain.model.AdminSubmissionBlocks.Verdicts;
import com.algoprep.identity.domain.model.StatSummary;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import org.springframework.stereotype.Service;

/**
 * Joins the judge counts with the problem-bank catalogue (DD api F1 section 0.8.5). The judge
 * counts only inside the scope identity hands it, so a withdrawn problem never reaches the level
 * groups or the popular list. Any failure of a source becomes one {@code 503 IDT-506} for that
 * block alone.
 *
 * <p>ponytail: no 60-second cache per block (DEC-2026-1107-f1-backend-clear-order defers it); add
 * one when several admins keep the overview open.
 */
@Service
class GetAdminSubmissionBlocksQueryHandler implements GetAdminSubmissionBlocksUseCase {
  static final List<String> DIFFICULTY_LEGEND = List.of("totalSubmissions", "acceptedSubmissions");

  private final JudgeStatsPort judge;
  private final ProblemBankStatsPort problems;

  GetAdminSubmissionBlocksQueryHandler(JudgeStatsPort judge, ProblemBankStatsPort problems) {
    this.judge = judge;
    this.problems = problems;
  }

  @Override
  public StatSummary submissionsSummary() {
    return read(judge::summary);
  }

  @Override
  public Languages byLanguage() {
    return read(judge::byLanguage);
  }

  @Override
  public Verdicts verdicts() {
    return read(judge::verdicts);
  }

  @Override
  public Difficulty byDifficulty() {
    return read(
        () -> {
          List<LevelRef> levels = problems.levelCatalog();
          Map<UUID, Set<UUID>> byLevel = new LinkedHashMap<>();
          levels.forEach(l -> byLevel.put(l.id(), new LinkedHashSet<>()));
          for (PublishedProblem p : problems.publishedProblems()) {
            Set<UUID> ids = byLevel.get(p.levelId());
            if (ids != null) {
              ids.add(p.id());
            }
          }
          List<LevelScope> scope =
              levels.stream().map(l -> new LevelScope(l.id(), byLevel.get(l.id()))).toList();
          Map<UUID, LevelCount> counted = new HashMap<>();
          judge.byDifficulty(scope).forEach(c -> counted.put(c.levelId(), c));
          List<DifficultyGroup> groups = new ArrayList<>(levels.size());
          for (LevelRef l : levels) {
            LevelCount c = counted.get(l.id());
            groups.add(
                new DifficultyGroup(
                    l.id(),
                    l.code(),
                    l.displayName(),
                    c == null ? 0 : c.submissions(),
                    c == null ? 0 : c.accepted()));
          }
          return new Difficulty(groups, DIFFICULTY_LEGEND);
        });
  }

  @Override
  public Days byDay() {
    return read(judge::byDay);
  }

  @Override
  public Months byMonth() {
    return read(judge::byMonth);
  }

  @Override
  public TopProblems topProblems() {
    return read(
        () -> {
          Map<UUID, PublishedProblem> published = new HashMap<>();
          problems.publishedProblems().forEach(p -> published.put(p.id(), p));
          List<TopProblem> items = new ArrayList<>(4);
          for (ProblemCount c : judge.topProblems(Set.copyOf(published.keySet()))) {
            PublishedProblem p = published.get(c.problemId());
            if (p != null) {
              items.add(new TopProblem(p.id(), p.title(), c.submissionCount()));
            }
          }
          return new TopProblems(items);
        });
  }

  private static <T> T read(Supplier<T> block) {
    try {
      return block.get();
    } catch (RuntimeException unavailable) {
      throw new ServiceUnavailableException(
          IdentityErrorCodes.DASHBOARD_SOURCE_UNAVAILABLE,
          "The overview block is temporarily unavailable",
          null);
    }
  }
}
