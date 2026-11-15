package com.algoprep.problembank.application.ports.in;

import com.algoprep.problembank.domain.model.ProblemBankStatsModels.LevelRef;
import com.algoprep.problembank.domain.model.ProblemBankStatsModels.ProblemStat;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Trusted read for identity's admin overview (DD F2 58.1, {@code ProblemBankStatsPort}); there is
 * no route. The published catalogue summary is {@code StudentCatalogUseCase.summary()}.
 */
public interface ProblemBankStatsUseCase {
  List<LevelRef> levelCatalog();

  /** Unknown ids are absent from the answer; withdrawn problems come back with published false. */
  List<ProblemStat> problemSummaries(Set<UUID> ids);

  /**
   * Every published, non-deleted problem. The admin overview needs the whole set to scope the judge
   * counts by level and to rank the popular problems.
   */
  List<ProblemStat> publishedProblems();
}
