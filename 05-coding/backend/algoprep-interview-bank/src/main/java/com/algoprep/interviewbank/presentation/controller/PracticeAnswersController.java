package com.algoprep.interviewbank.presentation.controller;

import com.algoprep.common.exception.InvalidRequestException;
import com.algoprep.interviewbank.application.dto.PracticeOutcome;
import com.algoprep.interviewbank.application.dto.PracticeSubmission;
import com.algoprep.interviewbank.application.ports.in.ListMyAnswerAttemptsUseCase;
import com.algoprep.interviewbank.application.ports.in.SubmitPracticeAnswerUseCase;
import com.algoprep.interviewbank.domain.model.AnswerAttempt;
import com.algoprep.interviewbank.domain.model.AttemptPage;
import com.algoprep.interviewbank.domain.model.Feedback;
import com.algoprep.interviewbank.domain.model.FeedbackCriterion;
import com.algoprep.interviewbank.domain.model.LearnerCategory;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.JsonNode;

/**
 * DD api F6 sections 6 and 7, practice mode. The caller id and category come from the token only;
 * the answer text is the one untrusted field and goes on as data.
 */
@RestController
@RequestMapping("/api/v1/interview-bank/questions/{questionId}/practice-answers")
class PracticeAnswersController {
  private final SubmitPracticeAnswerUseCase submit;
  private final ListMyAnswerAttemptsUseCase list;

  PracticeAnswersController(SubmitPracticeAnswerUseCase submit, ListMyAnswerAttemptsUseCase list) {
    this.submit = submit;
    this.list = list;
  }

  record CriterionDto(String criterionCode, int achievementPercent, BigDecimal weightPercent) {}

  record FeedbackDto(
      List<String> achievedPoints,
      List<String> missingPoints,
      List<String> improvementSuggestions,
      List<CriterionDto> criteria) {}

  record AttemptDto(
      UUID id,
      int attemptNo,
      Instant createdAt,
      String answerText,
      String feedbackStatus,
      FeedbackDto feedback,
      String feedbackErrorCode) {}

  record Attempts(List<AttemptDto> items, boolean hasMore, long totalAttempts) {}

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  AttemptDto submit(
      JwtAuthenticationToken caller, @PathVariable String questionId, HttpServletRequest request)
      throws IOException {
    UUID id = QuestionRequests.questionId(questionId);
    JsonNode field =
        QuestionRequests.body(request, QuestionRequests.QUESTION_BODY_LIMIT, "INB-V06")
            .path("answerText");
    PracticeOutcome outcome =
        submit.submit(
            new PracticeSubmission(
                QuestionRequests.actor(caller),
                category(caller),
                id,
                field.isString() ? field.asString() : null));
    return dto(outcome.attempt(), outcome.feedbackErrorCode());
  }

  @GetMapping
  Attempts list(
      JwtAuthenticationToken caller,
      @PathVariable String questionId,
      @RequestParam(name = "before", required = false) String before) {
    Integer cursor = null;
    if (before != null) {
      try {
        cursor = Integer.valueOf(before.strip());
      } catch (NumberFormatException failure) {
        throw new InvalidRequestException("INB-V08", "before must be a positive integer");
      }
      if (cursor <= 0) {
        throw new InvalidRequestException("INB-V08", "before must be a positive integer");
      }
    }
    AttemptPage page =
        list.list(QuestionRequests.actor(caller), QuestionRequests.questionId(questionId), cursor);
    return new Attempts(
        page.items().stream().map(a -> dto(a, null)).toList(),
        page.hasMore(),
        page.totalAttempts());
  }

  private static AttemptDto dto(AnswerAttempt a, String errorCode) {
    return new AttemptDto(
        a.id(),
        a.attemptNo(),
        a.createdAt(),
        a.answerText(),
        a.status().name(),
        a.feedback() == null ? null : feedback(a.feedback()),
        errorCode);
  }

  private static FeedbackDto feedback(Feedback f) {
    return new FeedbackDto(
        f.achievedPoints(),
        f.missingPoints(),
        f.improvementSuggestions(),
        f.criteria().stream().map(PracticeAnswersController::criterion).toList());
  }

  private static CriterionDto criterion(FeedbackCriterion c) {
    return new CriterionDto(c.criterionCode(), c.achievementPercent(), c.weightPercent());
  }

  private static LearnerCategory category(JwtAuthenticationToken caller) {
    var roles = caller.getAuthorities().stream().map(a -> a.getAuthority()).toList();
    if (roles.contains("ROLE_ADMIN")) {
      return LearnerCategory.ADMIN;
    }
    return roles.contains("ROLE_INSTRUCTOR") ? LearnerCategory.INSTRUCTOR : LearnerCategory.STUDENT;
  }
}
