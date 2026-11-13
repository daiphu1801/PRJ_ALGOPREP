package com.algoprep.problembank.domain.ports.out;

import com.algoprep.problembank.domain.model.SuggestedProblemModels.Candidate;
import java.util.List;
import java.util.UUID;

/** Published problems only ({@code PUBLISHED AND NOT deleted}), held in the query itself. */
public interface SuggestedProblemReadRepository {
  /**
   * Every problem that matches; a null {@code difficulty} or one that names no level means all
   * levels, a null {@code topic} means all topics.
   */
  List<Candidate> candidates(String difficulty, UUID topic);
}
