package com.algoprep.aireview.application.query;

import com.algoprep.aireview.application.dto.InterviewSummary;
import com.algoprep.aireview.application.ports.in.GetMyInterviewSummaryUseCase;
import com.algoprep.aireview.domain.exception.AiInvalidRequestException;
import com.algoprep.aireview.domain.model.InterviewResult;
import com.algoprep.aireview.domain.model.RubricCriterion;
import com.algoprep.aireview.domain.model.RubricRow;
import com.algoprep.aireview.domain.model.RubricScore;
import com.algoprep.aireview.domain.ports.out.InterviewScoringRepository;
import com.algoprep.common.domain.ports.out.ClockPort;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Endpoint 13: PostgreSQL only, computed in the application layer, no cache. A session counts only
 * when it is COMPLETED and has all four rubric rows (a half-scored session is ignored). The range
 * filters on {@code ended_at}.
 */
@Service
@Transactional(readOnly = true)
class GetMyInterviewSummaryQueryHandler implements GetMyInterviewSummaryUseCase {
  private final InterviewScoringRepository scoring;
  private final ClockPort clock;

  GetMyInterviewSummaryQueryHandler(InterviewScoringRepository scoring, ClockPort clock) {
    this.scoring = scoring;
    this.clock = clock;
  }

  @Override
  public InterviewSummary summary(UUID userId, String range, int limit) {
    Instant from =
        switch (range) {
          case "all" -> null;
          case "7d" -> clock.now().minus(Duration.ofDays(7));
          case "30d" -> clock.now().minus(Duration.ofDays(30));
          default -> throw new AiInvalidRequestException("AIR-V10", "range must be 7d, 30d or all");
        };
    Map<UUID, List<RubricRow>> bySession = new LinkedHashMap<>();
    for (RubricRow row : scoring.completedRubrics(userId, from)) {
      bySession.computeIfAbsent(row.sessionId(), id -> new ArrayList<>()).add(row);
    }
    List<InterviewSummary.Recent> complete = new ArrayList<>();
    for (List<RubricRow> rows : bySession.values()) {
      if (rows.size() == RubricCriterion.values().length) {
        complete.add(recent(rows));
      }
    }
    if (complete.isEmpty()) {
      return new InterviewSummary(0, null, null, List.of());
    }
    return new InterviewSummary(
        complete.size(),
        average(complete),
        weakest(complete),
        complete.stream().limit(limit).toList());
  }

  private static InterviewSummary.Recent recent(List<RubricRow> rows) {
    Map<RubricCriterion, BigDecimal> rubric = new EnumMap<>(RubricCriterion.class);
    List<RubricScore> scores = new ArrayList<>();
    for (RubricRow row : rows) {
      rubric.put(row.criterion(), row.score());
      scores.add(new RubricScore(row.criterion(), row.score(), "-"));
    }
    RubricRow first = rows.get(0);
    return new InterviewSummary.Recent(
        first.sessionId(),
        first.entryType(),
        first.startedAt(),
        first.endedAt(),
        InterviewResult.overall(scores),
        rubric);
  }

  private static BigDecimal average(List<InterviewSummary.Recent> sessions) {
    BigDecimal sum = BigDecimal.ZERO;
    for (InterviewSummary.Recent s : sessions) {
      sum = sum.add(s.overallScore());
    }
    return sum.divide(BigDecimal.valueOf(sessions.size()), 2, RoundingMode.HALF_UP);
  }

  /** Lowest average score; a tie goes to the criterion earlier in the fixed order. */
  private static RubricCriterion weakest(List<InterviewSummary.Recent> sessions) {
    RubricCriterion weakest = null;
    BigDecimal lowest = null;
    for (RubricCriterion criterion : RubricCriterion.values()) {
      BigDecimal sum = BigDecimal.ZERO;
      for (InterviewSummary.Recent s : sessions) {
        sum = sum.add(s.rubric().get(criterion));
      }
      if (lowest == null || sum.compareTo(lowest) < 0) {
        lowest = sum;
        weakest = criterion;
      }
    }
    return weakest;
  }
}
