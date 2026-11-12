package com.algoprep.bootstrap.interviewbank;

import com.algoprep.aireview.application.dto.PracticeFeedback;
import com.algoprep.aireview.application.ports.in.ComparePracticeAnswerUseCase;
import com.algoprep.aireview.application.ports.in.RefundPracticeFeedbackUseCase;
import com.algoprep.aireview.application.ports.in.ReservePracticeFeedbackUseCase;
import com.algoprep.aireview.domain.exception.AiBudgetExceededException;
import com.algoprep.aireview.domain.exception.AiInvalidResponseException;
import com.algoprep.aireview.domain.model.CallerCategory;
import com.algoprep.interviewbank.domain.exception.FeedbackInvalidException;
import com.algoprep.interviewbank.domain.exception.FeedbackUnavailableException;
import com.algoprep.interviewbank.domain.exception.PracticeBudgetLockedException;
import com.algoprep.interviewbank.domain.exception.PracticeQuotaExceededException;
import com.algoprep.interviewbank.domain.model.FeedbackRequest;
import com.algoprep.interviewbank.domain.model.FeedbackResult;
import com.algoprep.interviewbank.domain.model.GateTicket;
import com.algoprep.interviewbank.domain.model.LearnerCategory;
import com.algoprep.interviewbank.domain.model.RubricCriterion;
import com.algoprep.interviewbank.domain.ports.out.AnswerFeedbackPort;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Type translation only (DD api F6 sections 25 and 26): interview-bank asks, ai-review answers
 * through its P5 entries. The feature code PRACTICE_FEEDBACK is fixed inside ai-review, so this
 * bridge cannot charge the ledger to another feature. A failure of the AI side becomes the
 * unavailable error of F6, which stores the attempt as FAILED; F6 study mode never calls it.
 */
@Component
final class AnswerFeedbackBridge implements AnswerFeedbackPort {
  private static final Logger LOG = LoggerFactory.getLogger(AnswerFeedbackBridge.class);

  private final ReservePracticeFeedbackUseCase reserve;
  private final RefundPracticeFeedbackUseCase refund;
  private final ComparePracticeAnswerUseCase compare;

  AnswerFeedbackBridge(
      ReservePracticeFeedbackUseCase reserve,
      RefundPracticeFeedbackUseCase refund,
      ComparePracticeAnswerUseCase compare) {
    this.reserve = reserve;
    this.refund = refund;
    this.compare = compare;
  }

  @Override
  public GateTicket reserve(UUID userId, LearnerCategory category, int hourlyLimit) {
    PracticeFeedback.Ticket ticket;
    try {
      ticket = reserve.reserve(userId, CallerCategory.valueOf(category.name()), hourlyLimit);
    } catch (AiBudgetExceededException locked) {
      throw new PracticeBudgetLockedException();
    } catch (RuntimeException failure) {
      throw new FeedbackUnavailableException(failure);
    }
    if (ticket.status() == PracticeFeedback.Status.QUOTA_EXCEEDED) {
      throw new PracticeQuotaExceededException(ticket.retryAfterSeconds());
    }
    return new GateTicket(userId, ticket.requestId(), ticket.epochHour(), ticket.charged());
  }

  @Override
  public void refund(GateTicket ticket) {
    refund.refund(
        new PracticeFeedback.Ticket(
            ticket.charged() ? PracticeFeedback.Status.GRANTED : PracticeFeedback.Status.BYPASSED,
            ticket.userId(),
            ticket.requestId(),
            ticket.epochHour(),
            0));
  }

  @Override
  public FeedbackResult compareAnswer(FeedbackRequest request) {
    List<PracticeFeedback.Criterion> rubric =
        request.rubric().stream().map(AnswerFeedbackBridge::criterion).toList();
    PracticeFeedback.Output out;
    try {
      out =
          compare.compare(
              new PracticeFeedback.Input(
                  request.callId(),
                  request.userId(),
                  request.answerId(),
                  request.systemInstruction(),
                  "",
                  rubric,
                  request.questionTitle(),
                  request.questionContent(),
                  request.answerText()));
    } catch (AiInvalidResponseException invalid) {
      throw new FeedbackInvalidException(invalid);
    } catch (RuntimeException failure) {
      // Class and message only: never the answer text, the key or a stack with data.
      LOG.warn(
          "Practice feedback call failed: {}: {}",
          failure.getClass().getName(),
          failure.getMessage());
      throw new FeedbackUnavailableException(failure);
    }
    return new FeedbackResult(
        out.achievedPoints(),
        out.missingPoints(),
        out.improvementSuggestions(),
        out.criteria().stream()
            .map(s -> new FeedbackResult.CriterionScore(s.criterionCode(), s.achievementPercent()))
            .toList());
  }

  private static PracticeFeedback.Criterion criterion(RubricCriterion c) {
    return new PracticeFeedback.Criterion(c.criterionCode(), c.weightPercent(), c.description());
  }
}
