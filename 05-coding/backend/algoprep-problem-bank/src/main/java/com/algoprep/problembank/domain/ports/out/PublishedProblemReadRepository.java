package com.algoprep.problembank.domain.ports.out;

import com.algoprep.problembank.domain.model.student.PublishedProblemModels.Filter;
import com.algoprep.problembank.domain.model.student.PublishedProblemModels.Overview;
import com.algoprep.problembank.domain.model.student.PublishedProblemModels.Page;
import com.algoprep.problembank.domain.model.student.PublishedProblemModels.Paging;
import com.algoprep.problembank.domain.model.student.PublishedProblemModels.Samples;
import com.algoprep.problembank.domain.model.student.PublishedProblemModels.SolveStates;
import com.algoprep.problembank.domain.model.student.PublishedProblemModels.Workspace;
import java.util.Optional;
import java.util.UUID;

/**
 * Learner-facing reads. Every query holds {@code status = PUBLISHED AND NOT deleted} itself, so an
 * unpublished problem is simply not found. {@code states} is null when the solve state could not be
 * read; then a status condition is not applied.
 */
public interface PublishedProblemReadRepository {
  Page list(Filter filter, SolveStates states, Paging paging);

  Optional<UUID> random(Filter filter, SolveStates states);

  Optional<Workspace> workspace(UUID problemId, UUID userId);

  Optional<Overview> overview(UUID problemId, UUID userId);

  /**
   * Approved SAMPLE rows only (the condition is in the SQL); empty when the problem is not visible.
   */
  Optional<Samples> samples(UUID problemId);
}
