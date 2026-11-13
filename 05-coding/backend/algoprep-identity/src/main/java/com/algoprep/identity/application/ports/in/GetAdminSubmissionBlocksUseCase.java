package com.algoprep.identity.application.ports.in;

import com.algoprep.identity.domain.model.AdminSubmissionBlocks.Days;
import com.algoprep.identity.domain.model.AdminSubmissionBlocks.Difficulty;
import com.algoprep.identity.domain.model.AdminSubmissionBlocks.Languages;
import com.algoprep.identity.domain.model.AdminSubmissionBlocks.Months;
import com.algoprep.identity.domain.model.AdminSubmissionBlocks.TopProblems;
import com.algoprep.identity.domain.model.AdminSubmissionBlocks.Verdicts;
import com.algoprep.identity.domain.model.StatSummary;

/**
 * The judge-backed blocks of the admin overview (DD api F1 section 0.8.5): #43 and #67 to #72. Each
 * call stands alone, so one source failing costs only its own block.
 */
public interface GetAdminSubmissionBlocksUseCase {

  /** #43: total submissions. */
  StatSummary submissionsSummary();

  /** #67: submissions per language. */
  Languages byLanguage();

  /** #68: verdict distribution. */
  Verdicts verdicts();

  /** #69: submissions and accepted per level. */
  Difficulty byDifficulty();

  /** #70: submissions per day. */
  Days byDay();

  /** #71: submissions per month. */
  Months byMonth();

  /** #72: the most submitted published problems. */
  TopProblems topProblems();
}
