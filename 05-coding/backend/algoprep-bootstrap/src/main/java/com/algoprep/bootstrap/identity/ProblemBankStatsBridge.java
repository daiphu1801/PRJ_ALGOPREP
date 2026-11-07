package com.algoprep.bootstrap.identity;

import com.algoprep.identity.application.ports.out.ProblemBankStatsPort;
import com.algoprep.identity.domain.model.AdminSubmissionBlocks.LevelRef;
import com.algoprep.identity.domain.model.AdminSubmissionBlocks.PublishedProblem;
import com.algoprep.problembank.application.ports.in.ProblemBankStatsUseCase;
import java.time.Duration;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Type translation only: identity asks for the catalogue behind the admin overview, problem-bank
 * answers through {@link ProblemBankStatsUseCase} (DD api F2 section 58.1). Each read runs on a
 * virtual thread with a 2 second deadline; a failure is one {@link IllegalStateException}.
 */
@Component
public final class ProblemBankStatsBridge implements ProblemBankStatsPort {
  private final ProblemBankStatsUseCase stats;
  private final Duration limit;

  @Autowired
  public ProblemBankStatsBridge(ProblemBankStatsUseCase stats) {
    this(stats, Duration.ofSeconds(2));
  }

  ProblemBankStatsBridge(ProblemBankStatsUseCase stats, Duration limit) {
    this.stats = stats;
    this.limit = limit;
  }

  @Override
  public List<LevelRef> levelCatalog() {
    return BridgeDeadline.call(
        "problem-bank-stats-bridge",
        limit,
        () ->
            stats.levelCatalog().stream()
                .map(l -> new LevelRef(l.id(), l.code(), l.displayName()))
                .toList());
  }

  @Override
  public List<PublishedProblem> publishedProblems() {
    return BridgeDeadline.call(
        "problem-bank-stats-bridge",
        limit,
        () ->
            stats.publishedProblems().stream()
                .map(p -> new PublishedProblem(p.problemId(), p.title(), p.levelId()))
                .toList());
  }
}
