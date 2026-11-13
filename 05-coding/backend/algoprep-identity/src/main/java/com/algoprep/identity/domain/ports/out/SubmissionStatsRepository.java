package com.algoprep.identity.domain.ports.out;

import com.algoprep.identity.domain.model.GradedSubmission;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Read models fed by {@code SubmissionGraded}: best score, totals, language counts, daily count.
 */
public interface SubmissionStatsRepository {

  /** Keeps the higher ratio; the verdict stored is the one of the attempt holding that ratio. */
  void keepBestScore(UUID userId, UUID problemId, BigDecimal ratio, String verdict);

  /** Adds one submission to the totals and to the counter of its language, then fixes the top. */
  void addToTotals(GradedSubmission submission);

  /** Adds one to the count of {@code day} (the UTC+7 calendar day of the submission). */
  void addToDay(UUID userId, LocalDate day);
}
