package com.algoprep.aireview.presentation.controller;

import com.algoprep.aireview.application.dto.InterviewSummary;
import com.algoprep.aireview.application.ports.in.GetMyInterviewSummaryUseCase;
import com.algoprep.aireview.domain.exception.AiInvalidRequestException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** DD api F5 section 13: the caller's own mock-interview summary, never anyone else's. */
@RestController
@RequestMapping("/api/v1/ai/me/interview-sessions")
class InterviewSummaryController {
  private static final int DEFAULT_LIMIT = 5;
  private static final int MAX_LIMIT = 10;

  private final GetMyInterviewSummaryUseCase summary;

  InterviewSummaryController(GetMyInterviewSummaryUseCase summary) {
    this.summary = summary;
  }

  record RecentView(
      UUID sessionId,
      String entryType,
      Instant startedAt,
      Instant endedAt,
      BigDecimal overallScore,
      Map<String, BigDecimal> rubricScores) {}

  record SummaryResponse(
      int completedSessionCount,
      BigDecimal averageScore,
      String weakestCriterionCode,
      List<RecentView> recentSessions) {}

  @GetMapping("/summary")
  SummaryResponse get(
      JwtAuthenticationToken auth,
      @RequestParam(required = false) String range,
      @RequestParam(required = false) String limit) {
    InterviewSummary s =
        summary.summary(
            UUID.fromString(auth.getToken().getSubject()),
            range == null ? "all" : range,
            limit(limit));
    return new SummaryResponse(
        s.completedSessionCount(),
        s.averageScore(),
        s.weakest() == null ? null : s.weakest().name(),
        s.recent().stream().map(InterviewSummaryController::recent).toList());
  }

  private static RecentView recent(InterviewSummary.Recent r) {
    Map<String, BigDecimal> rubric = new LinkedHashMap<>();
    r.rubric().forEach((criterion, score) -> rubric.put(criterion.name(), score));
    return new RecentView(
        r.sessionId(), r.entryType().name(), r.startedAt(), r.endedAt(), r.overallScore(), rubric);
  }

  private static int limit(String raw) {
    if (raw == null) {
      return DEFAULT_LIMIT;
    }
    try {
      int value = Integer.parseInt(raw);
      if (value >= 1 && value <= MAX_LIMIT) {
        return value;
      }
    } catch (NumberFormatException notANumber) {
      // falls through to the same error as an out-of-range value
    }
    throw new AiInvalidRequestException("AIR-V02", "limit must be an integer from 1 to 10");
  }
}
