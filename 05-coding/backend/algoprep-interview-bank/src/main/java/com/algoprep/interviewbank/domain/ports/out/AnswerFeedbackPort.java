package com.algoprep.interviewbank.domain.ports.out;

import com.algoprep.interviewbank.domain.model.FeedbackRequest;
import com.algoprep.interviewbank.domain.model.FeedbackResult;
import com.algoprep.interviewbank.domain.model.GateTicket;
import com.algoprep.interviewbank.domain.model.LearnerCategory;
import java.util.UUID;

/**
 * Outbound port to the AI side (DD api F6 sections 25 and 26), implemented by a bridge in the
 * bootstrap module. The hourly slot, the shared budget and the token ledger all live behind it.
 */
public interface AnswerFeedbackPort {
  /**
   * Hourly slot first, then the shared budget. Throws {@code PracticeQuotaExceededException}
   * (INB-041), {@code PracticeBudgetLockedException} (INB-042) or {@code
   * FeedbackUnavailableException} when the gate itself fails.
   */
  GateTicket reserve(UUID userId, LearnerCategory category, int hourlyLimit);

  /** Gives the slot back; calling it twice with one ticket does nothing the second time. */
  void refund(GateTicket ticket);

  /**
   * One call, no retry. Throws {@code FeedbackUnavailableException} or {@code
   * FeedbackInvalidException}.
   */
  FeedbackResult compareAnswer(FeedbackRequest request);
}
