package com.algoprep.problembank.domain.ports.out;

import com.algoprep.problembank.domain.model.PublishedProblemModels.SolveStates;
import com.algoprep.problembank.domain.model.StudentCatalogModels.CatalogSummary;
import com.algoprep.problembank.domain.model.StudentCatalogModels.LevelProgress;
import com.algoprep.problembank.domain.model.StudentCatalogModels.TopicProgress;
import java.util.List;

/**
 * Counts over {@code PUBLISHED AND NOT deleted} problems only: a withdrawn problem is in neither
 * the numerator nor the denominator. {@code states} null means the solve state is unknown.
 */
public interface StudentCatalogReadRepository {
  /** Every topic by sort order, including those with no published problem. */
  List<TopicProgress> topicProgress(SolveStates states);

  /** Every level by sort order, including those with no published problem. */
  List<LevelProgress> levelProgress(SolveStates states);

  CatalogSummary summary();
}
