package com.algoprep.interviewbank.application.command;

import com.algoprep.common.domain.ports.out.IdGeneratorPort;
import com.algoprep.common.exception.DomainRuleViolationException;
import com.algoprep.common.exception.InvalidRequestException;
import com.algoprep.common.exception.ResourceConflictException;
import com.algoprep.common.exception.ResourceNotFoundException;
import com.algoprep.interviewbank.application.dto.PracticeOutcome;
import com.algoprep.interviewbank.application.dto.PracticeSubmission;
import com.algoprep.interviewbank.application.ports.in.SubmitPracticeAnswerUseCase;
import com.algoprep.interviewbank.domain.exception.AnswerTooLongException;
import com.algoprep.interviewbank.domain.exception.AttemptNumberConflictException;
import com.algoprep.interviewbank.domain.exception.FeedbackInvalidException;
import com.algoprep.interviewbank.domain.exception.PracticeBudgetLockedException;
import com.algoprep.interviewbank.domain.exception.PracticeQuotaExceededException;
import com.algoprep.interviewbank.domain.model.AnswerAttempt;
import com.algoprep.interviewbank.domain.model.Feedback;
import com.algoprep.interviewbank.domain.model.FeedbackCriterion;
import com.algoprep.interviewbank.domain.model.FeedbackRequest;
import com.algoprep.interviewbank.domain.model.FeedbackResult;
import com.algoprep.interviewbank.domain.model.FeedbackStatus;
import com.algoprep.interviewbank.domain.model.GateTicket;
import com.algoprep.interviewbank.domain.model.NewAttempt;
import com.algoprep.interviewbank.domain.model.PracticePolicy;
import com.algoprep.interviewbank.domain.model.PracticeQuestion;
import com.algoprep.interviewbank.domain.model.RubricCriterion;
import com.algoprep.interviewbank.domain.ports.out.AnswerFeedbackPort;
import com.algoprep.interviewbank.domain.ports.out.PracticeRepository;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.springframework.stereotype.Service;

/**
 * BR-IB-07 and BR-IB-08, in the order the DD fixes: validate, read the snapshot once, make the ids
 * before any gate, hourly slot and shared budget, one AI call outside any transaction, schema
 * check, then one short write transaction. An AI failure never loses the answer: the attempt is
 * stored FAILED. Deliberately not transactional itself, so no connection is held during the call.
 */
@Service
class SubmitPracticeAnswerCommandHandler implements SubmitPracticeAnswerUseCase {
  static final String UNAVAILABLE = "INB-501";
  static final String INVALID = "INB-502";
  private static final Logger LOG =
      Logger.getLogger(SubmitPracticeAnswerCommandHandler.class.getName());

  private final PracticeRepository repository;
  private final PracticeAttemptStore store;
  private final AnswerFeedbackPort ai;
  private final IdGeneratorPort ids;

  SubmitPracticeAnswerCommandHandler(
      PracticeRepository repository,
      PracticeAttemptStore store,
      AnswerFeedbackPort ai,
      IdGeneratorPort ids) {
    this.repository = repository;
    this.store = store;
    this.ai = ai;
    this.ids = ids;
  }

  /** Feedback or the error code that replaces it; exactly one of the two is set. */
  private record Judged(Feedback feedback, String errorCode) {}

  @Override
  public PracticeOutcome submit(PracticeSubmission in) {
    String text = checked(in.answerText());
    PracticeQuestion question =
        repository
            .findActiveQuestion(in.questionId())
            .orElseThrow(() -> new ResourceNotFoundException("INB-010", "Question not available"));
    if (question.rubric().isEmpty()) {
      throw new DomainRuleViolationException("INB-040", "Question has no rubric to practise");
    }
    UUID answerId = ids.newId();
    UUID callId = ids.newId();
    Judged judged = judge(in, question, text, answerId, callId);
    Feedback feedback = judged.feedback();
    AnswerAttempt saved =
        write(
            new NewAttempt(
                answerId,
                in.userId(),
                in.questionId(),
                text,
                feedback == null ? FeedbackStatus.FAILED : FeedbackStatus.COMPLETED,
                feedback,
                question.rubric()));
    return new PracticeOutcome(saved, judged.errorCode());
  }

  /** Hourly slot, shared budget, one model call, schema check; never throws for an AI failure. */
  private Judged judge(
      PracticeSubmission in, PracticeQuestion question, String text, UUID answerId, UUID callId) {
    GateTicket ticket;
    try {
      ticket = ai.reserve(in.userId(), in.category(), PracticePolicy.RATE_LIMIT_PER_HOUR);
    } catch (PracticeQuotaExceededException | PracticeBudgetLockedException refused) {
      throw refused;
    } catch (RuntimeException gateDown) {
      warn("gate", gateDown);
      return new Judged(null, UNAVAILABLE);
    }
    try {
      FeedbackResult result =
          ai.compareAnswer(
              new FeedbackRequest(
                  callId,
                  in.userId(),
                  answerId,
                  PracticePolicy.SYSTEM_INSTRUCTION,
                  question.rubric(),
                  question.title(),
                  question.contentMarkdown(),
                  text));
      return new Judged(withWeights(result, question.rubric()), null);
    } catch (FeedbackInvalidException invalid) {
      // Tokens were spent, so the hourly slot stays used (DD api F6 6.5 step 8).
      return new Judged(null, INVALID);
    } catch (RuntimeException unavailable) {
      warn("compare", unavailable);
      ai.refund(ticket);
      return new Judged(null, UNAVAILABLE);
    }
  }

  /** Class and message only: no stack, no answer text (the message never carries the request). */
  private static void warn(String step, RuntimeException failure) {
    LOG.log(
        Level.WARNING,
        "Practice feedback {0} failed: {1}: {2}",
        new Object[] {step, failure.getClass().getName(), failure.getMessage()});
  }

  /** Empty after trimming is INB-V06; the stored and sent text is the original, never edited. */
  private static String checked(String answerText) {
    if (answerText == null || answerText.isBlank()) {
      throw new InvalidRequestException("INB-V06", "answerText must not be empty");
    }
    if (answerText.length() > PracticePolicy.ANSWER_MAX_CHARS) {
      throw new AnswerTooLongException(PracticePolicy.ANSWER_MAX_CHARS);
    }
    return answerText;
  }

  /** Weights come from the snapshot, never from the AI (DD validation F6 section 7). */
  private static Feedback withWeights(FeedbackResult result, List<RubricCriterion> rubric) {
    Map<String, RubricCriterion> byCode = new HashMap<>();
    rubric.forEach(c -> byCode.put(c.criterionCode(), c));
    List<FeedbackCriterion> criteria = new ArrayList<>();
    for (FeedbackResult.CriterionScore score : result.criteria()) {
      RubricCriterion known = byCode.get(score.criterionCode());
      if (known == null) {
        throw new FeedbackInvalidException(null);
      }
      criteria.add(
          new FeedbackCriterion(
              known.criterionCode(), score.achievementPercent(), known.weightPercent()));
    }
    return new Feedback(
        result.achievedPoints(), result.missingPoints(), result.improvementSuggestions(), criteria);
  }

  private AnswerAttempt write(NewAttempt attempt) {
    for (int tries = 1; ; tries++) {
      try {
        return store.save(attempt);
      } catch (AttemptNumberConflictException conflict) {
        if (tries >= PracticePolicy.MAX_WRITE_ATTEMPTS) {
          LOG.log(
              Level.WARNING, "attempt number still taken after retries, answer {0}", attempt.id());
          throw new ResourceConflictException("INB-050", "Attempt number conflict");
        }
      }
    }
  }
}
