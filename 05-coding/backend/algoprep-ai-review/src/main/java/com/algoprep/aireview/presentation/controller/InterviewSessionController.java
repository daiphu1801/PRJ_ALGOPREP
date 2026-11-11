package com.algoprep.aireview.presentation.controller;

import com.algoprep.aireview.application.dto.InterviewHint;
import com.algoprep.aireview.application.dto.InterviewSessionDetail;
import com.algoprep.aireview.application.dto.OpenedInterviewSession;
import com.algoprep.aireview.application.ports.in.CompleteInterviewSessionUseCase;
import com.algoprep.aireview.application.ports.in.GetInterviewSessionUseCase;
import com.algoprep.aireview.application.ports.in.OpenInterviewSessionUseCase;
import com.algoprep.aireview.application.ports.in.RequestInterviewHintUseCase;
import com.algoprep.aireview.domain.exception.AiSessionNotFoundException;
import com.algoprep.aireview.domain.model.CallerCategory;
import com.algoprep.aireview.domain.model.InterviewResult;
import com.algoprep.aireview.domain.model.InterviewSession;
import com.algoprep.aireview.domain.model.InterviewTurn;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * DD api F5 sections 8, 9, 11 and 12. The caller id comes from the token, never from the request.
 * Every authenticated base category may call; ownership is decided by the use cases.
 */
@RestController
@RequestMapping("/api/v1/ai/interview-sessions")
class InterviewSessionController {
  private final OpenInterviewSessionUseCase open;
  private final GetInterviewSessionUseCase read;
  private final RequestInterviewHintUseCase hint;
  private final CompleteInterviewSessionUseCase complete;

  InterviewSessionController(
      OpenInterviewSessionUseCase open,
      GetInterviewSessionUseCase read,
      RequestInterviewHintUseCase hint,
      CompleteInterviewSessionUseCase complete) {
    this.open = open;
    this.read = read;
    this.hint = hint;
    this.complete = complete;
  }

  record SessionView(
      UUID sessionId,
      String entryType,
      String stage,
      int turnCount,
      Integer maxTurns,
      Boolean hintAllowed,
      String interviewerLevel,
      List<String> topics,
      UUID submissionId,
      String questionRef,
      Instant startedAt) {}

  record TurnView(int turnIndex, String role, String stage, String content, Instant createdAt) {}

  record OpenResponse(
      UUID sessionId,
      String entryType,
      String stage,
      int turnCount,
      Integer maxTurns,
      Boolean hintAllowed,
      String interviewerLevel,
      List<String> topics,
      UUID submissionId,
      String questionRef,
      Instant startedAt,
      TurnView openingMessage) {}

  record DetailResponse(
      SessionView session,
      Instant endedAt,
      String replyState,
      List<TurnView> turns,
      ResultView result) {}

  record CriterionView(String code, BigDecimal score, int weightPercent, String comment) {}

  /** Section 12.3; the same shape is nested in the endpoint 9 response. */
  record ResultView(
      UUID sessionId,
      String stage,
      boolean isReferenceOnly,
      BigDecimal overallScore,
      List<CriterionView> criteria,
      String feedbackSummary,
      int turnCount,
      Instant endedAt) {}

  record HintResponse(String hint, String stage) {}

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  OpenResponse start(
      JwtAuthenticationToken auth, @RequestBody InterviewRequests.StartRequest body) {
    OpenedInterviewSession opened =
        open.open(InterviewRequests.toCommand(body, caller(auth), category(auth)));
    InterviewSession s = opened.session();
    return new OpenResponse(
        s.id(),
        s.entryType().name(),
        s.stage().name(),
        s.turnCount(),
        s.maxTurns(),
        s.hintAllowed(),
        s.interviewerLevel() == null ? null : s.interviewerLevel().name(),
        topics(s),
        s.submissionId(),
        s.questionRef(),
        s.startedAt(),
        turn(opened.opening()));
  }

  @GetMapping("/{sessionId}")
  DetailResponse detail(JwtAuthenticationToken auth, @PathVariable String sessionId) {
    InterviewSessionDetail detail = read.get(caller(auth), sessionId(sessionId));
    InterviewSession s = detail.session();
    return new DetailResponse(
        new SessionView(
            s.id(),
            s.entryType().name(),
            s.stage().name(),
            s.turnCount(),
            s.maxTurns(),
            s.hintAllowed(),
            s.interviewerLevel() == null ? null : s.interviewerLevel().name(),
            topics(s),
            s.submissionId(),
            s.questionRef(),
            s.startedAt()),
        s.endedAt(),
        detail.replyState().name(),
        detail.turns().stream().map(InterviewSessionController::turn).toList(),
        detail.result() == null ? null : result(detail.result()));
  }

  @PostMapping("/{sessionId}/hint")
  HintResponse requestHint(JwtAuthenticationToken auth, @PathVariable String sessionId) {
    InterviewHint result = hint.hint(caller(auth), sessionId(sessionId));
    return new HintResponse(result.hint(), result.stage().name());
  }

  @PostMapping("/{sessionId}/complete")
  ResultView complete(JwtAuthenticationToken auth, @PathVariable String sessionId) {
    return result(complete.complete(caller(auth), sessionId(sessionId)));
  }

  private static ResultView result(InterviewResult r) {
    return new ResultView(
        r.sessionId(),
        "COMPLETED",
        true,
        r.overallScore(),
        r.criteria().stream()
            .map(
                c ->
                    new CriterionView(
                        c.criterion().name(),
                        c.score(),
                        c.criterion().weightPercent(),
                        c.comment()))
            .toList(),
        r.feedbackSummary(),
        r.turnCount(),
        r.endedAt());
  }

  private static List<String> topics(InterviewSession s) {
    return s.topics().isEmpty() ? null : s.topics();
  }

  private static TurnView turn(InterviewTurn t) {
    return new TurnView(
        t.turnIndex(), t.role().name(), t.stage().name(), t.content(), t.createdAt());
  }

  private static UUID sessionId(String raw) {
    try {
      return UUID.fromString(raw);
    } catch (IllegalArgumentException invalid) {
      throw new AiSessionNotFoundException();
    }
  }

  private static UUID caller(JwtAuthenticationToken auth) {
    return UUID.fromString(auth.getToken().getSubject());
  }

  static CallerCategory category(JwtAuthenticationToken auth) {
    var roles = auth.getAuthorities().stream().map(a -> a.getAuthority()).toList();
    if (roles.contains("ROLE_ADMIN")) {
      return CallerCategory.ADMIN;
    }
    return roles.contains("ROLE_INSTRUCTOR") ? CallerCategory.INSTRUCTOR : CallerCategory.STUDENT;
  }
}
