package com.algoprep.identity.application.ports.out;

import com.algoprep.identity.domain.model.AdminSubmissionBlocks.Days;
import com.algoprep.identity.domain.model.AdminSubmissionBlocks.Languages;
import com.algoprep.identity.domain.model.AdminSubmissionBlocks.LevelCount;
import com.algoprep.identity.domain.model.AdminSubmissionBlocks.LevelScope;
import com.algoprep.identity.domain.model.AdminSubmissionBlocks.Months;
import com.algoprep.identity.domain.model.AdminSubmissionBlocks.ProblemCount;
import com.algoprep.identity.domain.model.AdminSubmissionBlocks.Verdicts;
import com.algoprep.identity.domain.model.StatSummary;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Submission statistics of the admin overview, answered by the judge module (DD api F1 section
 * 0.8.5). Every method throws when the judge cannot answer; an empty answer is a real answer.
 */
public interface JudgeStatsPort {

  /** #43: all submissions with the 30-day comparison and a seven-point sparkline. */
  StatSummary summary();

  /** #67: the last 20 days per language. */
  Languages byLanguage();

  /** #68: the five final verdicts. */
  Verdicts verdicts();

  /** #69: counts per level, same order as {@code levels}, empty levels included. */
  List<LevelCount> byDifficulty(List<LevelScope> levels);

  /** #70: the last seven days. */
  Days byDay();

  /** #71: the last six months. */
  Months byMonth();

  /** #72: the four most submitted problems among {@code eligibleProblemIds}. */
  List<ProblemCount> topProblems(Set<UUID> eligibleProblemIds);
}
