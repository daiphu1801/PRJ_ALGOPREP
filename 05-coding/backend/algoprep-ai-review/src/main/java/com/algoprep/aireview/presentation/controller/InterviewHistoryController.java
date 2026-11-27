package com.algoprep.aireview.presentation.controller;

import com.algoprep.aireview.application.dto.interview.MySessionFilter;
import com.algoprep.aireview.application.dto.interview.MySessionFilter.ScoreBand;
import com.algoprep.aireview.application.dto.interview.MySessionFilter.SortField;
import com.algoprep.aireview.application.dto.interview.MySessionPage;
import com.algoprep.aireview.application.ports.in.interview.ListMyInterviewSessionsUseCase;
import com.algoprep.aireview.domain.exception.AiInvalidRequestException;
import com.algoprep.aireview.domain.model.interview.InterviewEntryType;
import com.algoprep.aireview.domain.model.interview.InterviewSessionStage;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * DD api F5 section 31A: the caller's own COMPLETED and ABANDONED sessions as a searchable,
 * filterable, sortable page. Never anyone else's sessions.
 */
@RestController
@RequestMapping("/api/v1/ai/me/interview-sessions")
class InterviewHistoryController {
  private static final int DEFAULT_SIZE = 20;
  private static final int MAX_SIZE = 50;
  private static final int MAX_SEARCH = 100;
  private static final List<String> LEVELS = List.of("INTERN", "JUNIOR", "MIDDLE", "SENIOR");

  private final ListMyInterviewSessionsUseCase sessions;

  InterviewHistoryController(ListMyInterviewSessionsUseCase sessions) {
    this.sessions = sessions;
  }

  record Item(
      UUID sessionId,
      String entryType,
      String status,
      String questionText,
      List<String> topics,
      String interviewerLevel,
      int turnCount,
      Integer maxTurns,
      Instant startedAt,
      Instant endedAt,
      BigDecimal overallScore) {}

  record PageMeta(int number, int size, long totalItems, int totalPages) {}

  record HistoryResponse(List<Item> items, PageMeta page) {}

  /** The query parameters are read from one map: ten separate arguments would be unreadable. */
  @GetMapping
  HistoryResponse list(JwtAuthenticationToken auth, @RequestParam Map<String, String> params) {
    int pageNumber = positive(params.get("page"), 1, Integer.MAX_VALUE);
    int pageSize = positive(params.get("size"), DEFAULT_SIZE, MAX_SIZE);
    MySessionFilter filter =
        new MySessionFilter(
            UUID.fromString(auth.getToken().getSubject()),
            search(params.get("q")),
            entry(params.get("entryType")),
            stage(params.get("status")),
            level(params.get("level")),
            band(params.get("scoreBand")),
            sortField(params.get("sort")),
            descending(params.get("order")),
            pageNumber,
            pageSize);
    MySessionPage result = sessions.list(filter);
    int totalPages = (int) Math.ceil(result.totalItems() / (double) pageSize);
    return new HistoryResponse(
        result.items().stream().map(InterviewHistoryController::item).toList(),
        new PageMeta(pageNumber, pageSize, result.totalItems(), totalPages));
  }

  private static Item item(MySessionPage.Row r) {
    return new Item(
        r.sessionId(),
        r.entryType().name(),
        r.stage().name(),
        r.questionText(),
        r.topics(),
        r.interviewerLevel(),
        r.turnCount(),
        r.maxTurns(),
        r.startedAt(),
        r.endedAt(),
        r.overallScore());
  }

  /**
   * Blank means no search; longer than 100 characters is cut, not refused (the screen also caps
   * it).
   */
  private static String search(String raw) {
    if (raw == null || raw.isBlank()) {
      return null;
    }
    String trimmed = raw.trim();
    return trimmed.length() > MAX_SEARCH ? trimmed.substring(0, MAX_SEARCH) : trimmed;
  }

  private static int positive(String raw, int fallback, int max) {
    if (raw == null) {
      return fallback;
    }
    try {
      int value = Integer.parseInt(raw);
      if (value >= 1 && value <= max) {
        return value;
      }
    } catch (NumberFormatException notANumber) {
      // falls through to the same error as an out-of-range value
    }
    throw new AiInvalidRequestException(
        "AIR-V20", "page must be 1 or more and size an integer from 1 to " + MAX_SIZE);
  }

  private static InterviewEntryType entry(String raw) {
    return raw == null ? null : parse(InterviewEntryType.class, raw, "entryType");
  }

  /** Only the two finished stages are listed; a running stage is refused like any unknown value. */
  private static InterviewSessionStage stage(String raw) {
    if (raw == null) {
      return null;
    }
    InterviewSessionStage stage = parse(InterviewSessionStage.class, raw, "status");
    if (stage.running()) {
      throw invalid("status");
    }
    return stage;
  }

  private static String level(String raw) {
    if (raw == null) {
      return null;
    }
    String upper = raw.toUpperCase(Locale.ROOT);
    if (!LEVELS.contains(upper)) {
      throw invalid("level");
    }
    return upper;
  }

  private static ScoreBand band(String raw) {
    return raw == null ? null : parse(ScoreBand.class, raw, "scoreBand");
  }

  private static SortField sortField(String raw) {
    if (raw == null || "startedAt".equals(raw)) {
      return SortField.STARTED_AT;
    }
    if ("score".equals(raw)) {
      return SortField.SCORE;
    }
    throw invalid("sort");
  }

  private static boolean descending(String raw) {
    if (raw == null || "desc".equalsIgnoreCase(raw)) {
      return true;
    }
    if ("asc".equalsIgnoreCase(raw)) {
      return false;
    }
    throw invalid("order");
  }

  private static <E extends Enum<E>> E parse(Class<E> type, String raw, String field) {
    try {
      return Enum.valueOf(type, raw.toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException unknown) {
      throw invalid(field);
    }
  }

  private static AiInvalidRequestException invalid(String field) {
    return new AiInvalidRequestException("AIR-V21", field + " has a value this list does not know");
  }
}
