package com.algoprep.problembank.application.query;

import com.algoprep.problembank.application.ports.in.ProblemBankStatsUseCase;
import com.algoprep.problembank.domain.model.ProblemBankStatsModels.LevelRef;
import com.algoprep.problembank.domain.model.ProblemBankStatsModels.ProblemStat;
import com.algoprep.problembank.domain.ports.out.ProblemBankStatsRepository;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Own read-only transaction, like the other trusted reads for neighbours, so a failed read cannot
 * mark the transaction of the caller for rollback.
 */
@Service
@Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
class ProblemBankStatsQueryHandler implements ProblemBankStatsUseCase {
  private final ProblemBankStatsRepository repository;

  ProblemBankStatsQueryHandler(ProblemBankStatsRepository repository) {
    this.repository = repository;
  }

  @Override
  public List<LevelRef> levelCatalog() {
    return repository.levels();
  }

  @Override
  public List<ProblemStat> problemSummaries(Set<UUID> ids) {
    return ids.isEmpty() ? List.of() : repository.problems(Set.copyOf(ids));
  }

  @Override
  public List<ProblemStat> publishedProblems() {
    return repository.published();
  }
}
