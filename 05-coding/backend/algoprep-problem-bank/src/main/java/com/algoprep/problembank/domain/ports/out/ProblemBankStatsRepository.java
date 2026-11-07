package com.algoprep.problembank.domain.ports.out;

import com.algoprep.problembank.domain.model.ProblemBankStatsModels.LevelRef;
import com.algoprep.problembank.domain.model.ProblemBankStatsModels.ProblemStat;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Metadata only; a withdrawn problem is still answered, flagged as not published. */
public interface ProblemBankStatsRepository {
  /** Every level by sort order, including levels without a problem. */
  List<LevelRef> levels();

  List<ProblemStat> problems(Set<UUID> ids);

  /** Every problem that is {@code PUBLISHED AND NOT deleted}, by code then id. */
  List<ProblemStat> published();
}
