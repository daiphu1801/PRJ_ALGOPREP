package com.algoprep.identity.application.ports.out;

import com.algoprep.identity.domain.model.AdminSubmissionBlocks.LevelRef;
import com.algoprep.identity.domain.model.AdminSubmissionBlocks.PublishedProblem;
import java.util.List;

/**
 * Catalogue data of the admin overview, answered by the problem-bank module (DD api F1 section
 * 0.8.5, DD api F2 section 58.1). Every method throws when problem-bank cannot answer.
 */
public interface ProblemBankStatsPort {

  /** Every level by {@code sort_order}, including levels without a problem. */
  List<LevelRef> levelCatalog();

  /** Every problem that is published and not deleted. */
  List<PublishedProblem> publishedProblems();
}
