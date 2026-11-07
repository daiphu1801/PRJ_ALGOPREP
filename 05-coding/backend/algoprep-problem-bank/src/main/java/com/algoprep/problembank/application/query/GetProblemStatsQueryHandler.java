package com.algoprep.problembank.application.query;

import com.algoprep.common.exception.ResourceNotFoundException;
import com.algoprep.problembank.application.dto.ProblemAuthoringQuery;
import com.algoprep.problembank.application.dto.ProblemStatsView;
import com.algoprep.problembank.application.ports.in.GetProblemStatsUseCase;
import com.algoprep.problembank.domain.ports.out.ProblemStatsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * DD F2 section 41. Access as section 34: a missing or deleted problem, or an instructor naming
 * someone else's draft, is the same 404 PRB-010. A problem with no counters yet answers zero.
 */
@Service
class GetProblemStatsQueryHandler implements GetProblemStatsUseCase {
  private final ProblemStatsRepository repository;

  GetProblemStatsQueryHandler(ProblemStatsRepository repository) {
    this.repository = repository;
  }

  @Override
  @Transactional(readOnly = true)
  public ProblemStatsView get(ProblemAuthoringQuery query) {
    var row =
        repository
            .find(query.problemId())
            .filter(
                r ->
                    query.admin()
                        || r.authorId().equals(query.actorId())
                        || "PUBLISHED".equals(r.status()))
            .orElseThrow(() -> new ResourceNotFoundException("PRB-010", "Problem not found"));
    return new ProblemStatsView(row.submissionCount(), row.acRate(), row.updatedAt());
  }
}
