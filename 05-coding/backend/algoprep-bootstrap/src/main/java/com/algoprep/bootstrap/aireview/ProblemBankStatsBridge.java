package com.algoprep.bootstrap.aireview;

import com.algoprep.aireview.application.ports.out.ProblemBankStatsPort;
import com.algoprep.aireview.domain.model.ProblemSummary;
import com.algoprep.bootstrap.identity.BridgeDeadline;
import com.algoprep.problembank.application.ports.in.ProblemBankStatsUseCase;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Type translation only: ai-review asks for problem titles, problem-bank answers through {@link
 * ProblemBankStatsUseCase}. The read runs under a 2 second deadline; a failure is one {@link
 * IllegalStateException}, which the review read handler turns into null titles.
 */
@Component("aiReviewProblemBankStatsBridge")
final class ProblemBankStatsBridge implements ProblemBankStatsPort {
  private final ProblemBankStatsUseCase stats;
  private final Duration limit;

  @Autowired
  ProblemBankStatsBridge(ProblemBankStatsUseCase stats) {
    this(stats, Duration.ofSeconds(2));
  }

  ProblemBankStatsBridge(ProblemBankStatsUseCase stats, Duration limit) {
    this.stats = stats;
    this.limit = limit;
  }

  @Override
  public List<ProblemSummary> problemSummaries(Set<UUID> problemIds) {
    return BridgeDeadline.call(
        "ai-review-problem-stats-bridge",
        limit,
        () ->
            stats.problemSummaries(problemIds).stream()
                .map(
                    p ->
                        new ProblemSummary(
                            p.problemId(), p.title(), p.levelCode(), p.levelDisplayName()))
                .toList());
  }
}
