package com.algoprep.aireview.application.query;

import static java.util.stream.Collectors.toSet;

import com.algoprep.aireview.application.ports.in.GetMyRecentSolutionReviewsUseCase;
import com.algoprep.aireview.application.ports.in.GetMySolutionReviewQuickInsightUseCase;
import com.algoprep.aireview.application.ports.in.ListMyReviewedProblemIdsUseCase;
import com.algoprep.aireview.application.ports.out.ProblemBankStatsPort;
import com.algoprep.aireview.domain.exception.AiInvalidRequestException;
import com.algoprep.aireview.domain.model.ProblemSummary;
import com.algoprep.aireview.domain.model.RecentSolutionReview;
import com.algoprep.aireview.domain.ports.out.SolutionReviewRepository;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Endpoints 5, 6 and 7: stored data only, scoped by the caller id, no LLM and no cost gate, so they
 * keep working while the provider is down or the budget is locked.
 */
@Service
@Transactional(readOnly = true)
class SolutionReviewReadHandlers
    implements ListMyReviewedProblemIdsUseCase,
        GetMySolutionReviewQuickInsightUseCase,
        GetMyRecentSolutionReviewsUseCase {
  static final int MAX_LIMIT = 10;
  private final SolutionReviewRepository reviews;
  private final ProblemBankStatsPort problems;

  SolutionReviewReadHandlers(SolutionReviewRepository reviews, ProblemBankStatsPort problems) {
    this.reviews = reviews;
    this.problems = problems;
  }

  @Override
  public List<UUID> list(UUID callerId) {
    return reviews.distinctProblemIds(callerId);
  }

  @Override
  public Optional<RecentSolutionReview> get(UUID callerId, UUID problemId) {
    return reviews.latestForProblem(callerId, problemId);
  }

  @Override
  public List<RecentSolutionReview> recent(UUID callerId, int limit) {
    if (limit < 1 || limit > MAX_LIMIT) {
      throw new AiInvalidRequestException("AIR-V02", "limit must be between 1 and 10");
    }
    List<RecentSolutionReview> rows = reviews.recent(callerId, limit);
    if (rows.isEmpty()) {
      return rows;
    }
    Map<UUID, String> titles = titles(rows);
    return rows.stream().map(r -> r.withProblemTitle(titles.get(r.problemId()))).toList();
  }

  /** One call for all rows; a failure only costs the titles (DD F5 section 7.3). */
  private Map<UUID, String> titles(List<RecentSolutionReview> rows) {
    try {
      Set<UUID> ids = rows.stream().map(RecentSolutionReview::problemId).collect(toSet());
      Map<UUID, String> byId = new HashMap<>();
      for (ProblemSummary p : problems.problemSummaries(ids)) {
        byId.put(p.problemId(), p.title());
      }
      return byId;
    } catch (RuntimeException unavailable) {
      return Map.of();
    }
  }
}
