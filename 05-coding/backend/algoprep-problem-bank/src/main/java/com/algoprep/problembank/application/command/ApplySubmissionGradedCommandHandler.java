package com.algoprep.problembank.application.command;

import com.algoprep.problembank.application.ports.in.ApplySubmissionGradedUseCase;
import com.algoprep.problembank.domain.model.ProblemStatsModels.GradedSubmission;
import com.algoprep.problembank.domain.ports.out.ProblemStatsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * BR-20. The event id and the counters change in one transaction, so a redelivery after a commit
 * counts nothing and a rollback leaves the id unrecorded for the next delivery.
 */
@Service("problemApplySubmissionGradedHandler")
class ApplySubmissionGradedCommandHandler implements ApplySubmissionGradedUseCase {
  private static final Logger LOG =
      LoggerFactory.getLogger(ApplySubmissionGradedCommandHandler.class);

  private final ProblemStatsRepository stats;

  ApplySubmissionGradedCommandHandler(ProblemStatsRepository stats) {
    this.stats = stats;
  }

  @Override
  @Transactional
  public boolean apply(GradedSubmission s) {
    if (!stats.markConsumed(s.eventId())) {
      return false;
    }
    // A system error is not an attempt at the problem (BR-20 step 2); the id stays recorded.
    if ("SYSTEM_ERROR".equals(s.verdict())) {
      return true;
    }
    if (!stats.addSubmission(s.problemId(), "ACCEPTED".equals(s.verdict()))) {
      LOG.warn("SubmissionGraded names a problem that does not exist; event skipped");
    }
    return true;
  }
}
