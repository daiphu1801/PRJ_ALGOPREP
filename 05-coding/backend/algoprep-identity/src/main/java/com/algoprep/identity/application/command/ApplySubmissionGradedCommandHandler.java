package com.algoprep.identity.application.command;

import com.algoprep.identity.application.ports.in.ApplySubmissionGradedUseCase;
import com.algoprep.identity.domain.model.GradedSubmission;
import com.algoprep.identity.domain.ports.out.ProcessedEventRepository;
import com.algoprep.identity.domain.ports.out.SubmissionStatsRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneOffset;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * DD api F1 section 2. Everything runs in one transaction together with the processed-event row, so
 * a redelivery never counts a submission twice. The day is cut at UTC+7 from {@code submittedAt}.
 */
@Service
class ApplySubmissionGradedCommandHandler implements ApplySubmissionGradedUseCase {
  static final String EVENT_TYPE = "SubmissionGraded";
  private static final ZoneOffset VIETNAM = ZoneOffset.ofHours(7);

  private final ProcessedEventRepository processed;
  private final SubmissionStatsRepository stats;

  ApplySubmissionGradedCommandHandler(
      ProcessedEventRepository processed, SubmissionStatsRepository stats) {
    this.processed = processed;
    this.stats = stats;
  }

  @Override
  @Transactional
  public boolean apply(GradedSubmission s) {
    if (!processed.markProcessed(s.eventId(), EVENT_TYPE)) {
      return false;
    }
    // A system error is not an attempt at the problem: no best score. A compile error has no
    // ratio and counts as 0 [SoT: Suy luận, DD gives null and the table needs a number].
    if (!"SYSTEM_ERROR".equals(s.verdict())) {
      stats.keepBestScore(
          s.userId(), s.problemId(), s.ratio() == null ? BigDecimal.ZERO : s.ratio(), s.verdict());
    }
    stats.addToTotals(s);
    stats.addToDay(s.userId(), LocalDate.ofInstant(s.submittedAt(), VIETNAM));
    return true;
  }
}
