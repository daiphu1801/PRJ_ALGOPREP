package com.algoprep.interviewbank.presentation.controller;

import com.algoprep.common.exception.InvalidRequestException;
import com.algoprep.interviewbank.application.dto.LearnerQuestionQuery;
import com.algoprep.interviewbank.application.ports.in.GetInterviewQuestionUseCase;
import com.algoprep.interviewbank.application.ports.in.ListInterviewQuestionsUseCase;
import com.algoprep.interviewbank.application.ports.in.RateQuestionRecallUseCase;
import com.algoprep.interviewbank.application.ports.in.SetQuestionBookmarkUseCase;
import com.algoprep.interviewbank.domain.model.LearnerQuestion;
import com.algoprep.interviewbank.domain.model.LearnerQuestionPage;
import com.algoprep.interviewbank.domain.model.LearnerQuestionRow;
import com.algoprep.interviewbank.domain.model.RecallFilter;
import com.algoprep.interviewbank.domain.model.RecallRating;
import com.algoprep.interviewbank.domain.model.RecallState;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.JsonNode;

/**
 * DD api F6 sections 2 to 5, study mode. Any authenticated user may call these; the caller id comes
 * from the token only and no AI collaborator is injected here.
 */
@RestController
@RequestMapping("/api/v1/interview-bank/questions")
class InterviewQuestionsController {
  private static final int BODY_LIMIT = 4_096;

  private final ListInterviewQuestionsUseCase list;
  private final GetInterviewQuestionUseCase get;
  private final RateQuestionRecallUseCase rate;
  private final SetQuestionBookmarkUseCase bookmark;

  InterviewQuestionsController(
      ListInterviewQuestionsUseCase list,
      GetInterviewQuestionUseCase get,
      RateQuestionRecallUseCase rate,
      SetQuestionBookmarkUseCase bookmark) {
    this.list = list;
    this.get = get;
    this.rate = rate;
    this.bookmark = bookmark;
  }

  record Ref(String code, String displayName) {}

  record Item(
      UUID id, Ref topic, Ref level, String title, RecallRating recallRating, boolean bookmarked) {}

  record DrillItem(
      UUID id,
      Ref topic,
      Ref level,
      String title,
      RecallRating recallRating,
      boolean bookmarked,
      String suggestedApproach,
      List<String> coreKeywords) {}

  record Page(int number, int size, long totalItems, int totalPages) {}

  record Meta(long shown, long total) {}

  record Items(List<?> items, Page page, Meta meta) {}

  record TopicRef(String code, String displayName, boolean usesStarFramework) {}

  record Recall(RecallRating rating, int intervalDays, Instant nextReviewAt, Instant ratedAt) {}

  record Detail(
      UUID id,
      TopicRef topic,
      Ref level,
      String title,
      String contentMarkdown,
      String suggestedApproach,
      String sampleAnswerFramework,
      List<String> coreKeywords,
      List<String> followUpQuestions,
      boolean bookmarked,
      boolean practiceAvailable,
      Recall recall) {}

  record Bookmark(UUID questionId, boolean bookmarked) {}

  private static UUID userId(JwtAuthenticationToken caller) {
    return UUID.fromString(caller.getToken().getSubject());
  }

  @GetMapping
  Items list(JwtAuthenticationToken caller, @RequestParam Map<String, String> params) {
    boolean drill = "true".equalsIgnoreCase(params.get("drill"));
    LearnerQuestionPage result =
        list.list(
            new LearnerQuestionQuery(
                userId(caller),
                params.get("q"),
                status(params.get("status")),
                blankToNull(params.get("topic")),
                blankToNull(params.get("level")),
                "true".equalsIgnoreCase(params.get("bookmarkedOnly")),
                integer(params.get("page"), 1),
                drill,
                integer(params.get("limit"), 10)));
    long shown = result.shown();
    int pages = drill ? 1 : (int) Math.max(1, (shown + result.size() - 1) / result.size());
    return new Items(
        result.items().stream().map(row -> item(row, drill)).toList(),
        new Page(result.number(), result.size(), shown, pages),
        new Meta(shown, result.total()));
  }

  private static Object item(LearnerQuestionRow r, boolean drill) {
    var topic = new Ref(r.topicCode(), r.topicName());
    var level = new Ref(r.levelCode(), r.levelName());
    return drill
        ? new DrillItem(
            r.id(),
            topic,
            level,
            r.title(),
            r.recall(),
            r.bookmarked(),
            r.suggestedApproach(),
            r.coreKeywords())
        : new Item(r.id(), topic, level, r.title(), r.recall(), r.bookmarked());
  }

  @GetMapping("/{questionId}")
  Detail get(JwtAuthenticationToken caller, @PathVariable String questionId) {
    LearnerQuestion q = get.get(userId(caller), QuestionRequests.questionId(questionId));
    return new Detail(
        q.id(),
        new TopicRef(q.topicCode(), q.topicName(), q.usesStarFramework()),
        new Ref(q.levelCode(), q.levelName()),
        q.title(),
        q.contentMarkdown(),
        q.suggestedApproach(),
        q.sampleAnswerFramework(),
        q.coreKeywords(),
        q.followUps(),
        q.bookmarked(),
        q.practiceAvailable(),
        recall(q.recall()));
  }

  @PutMapping("/{questionId}/recall")
  Recall rate(
      JwtAuthenticationToken caller, @PathVariable String questionId, HttpServletRequest request)
      throws IOException {
    UUID id = QuestionRequests.questionId(questionId);
    JsonNode field = QuestionRequests.body(request, BODY_LIMIT, "INB-V04").path("rating");
    RecallRating rating = null;
    if (field.isString()) {
      for (RecallRating candidate : RecallRating.values()) {
        if (candidate.name().equals(field.asString())) {
          rating = candidate;
        }
      }
    }
    if (rating == null) {
      throw new InvalidRequestException("INB-V04", "rating must be KNOWN, VAGUE or FORGOTTEN");
    }
    return recall(rate.rate(userId(caller), id, rating));
  }

  @PutMapping("/{questionId}/bookmark")
  Bookmark bookmark(
      JwtAuthenticationToken caller, @PathVariable String questionId, HttpServletRequest request)
      throws IOException {
    UUID id = QuestionRequests.questionId(questionId);
    JsonNode field = QuestionRequests.body(request, BODY_LIMIT, "INB-V05").path("bookmarked");
    if (!field.isBoolean()) {
      throw new InvalidRequestException("INB-V05", "bookmarked must be a boolean");
    }
    bookmark.set(userId(caller), id, field.asBoolean());
    return new Bookmark(id, field.asBoolean());
  }

  private static Recall recall(RecallState state) {
    return state == null
        ? null
        : new Recall(state.rating(), state.intervalDays(), state.nextReviewAt(), state.ratedAt());
  }

  private static RecallFilter status(String text) {
    if (text != null) {
      for (RecallFilter candidate : RecallFilter.values()) {
        if (candidate.name().equals(text.strip())) {
          return candidate;
        }
      }
    }
    return RecallFilter.ALL;
  }

  /** Bad numbers fall back to the default; the caller clamps ranges. */
  private static int integer(String text, int fallback) {
    try {
      return text == null ? fallback : Integer.parseInt(text.strip());
    } catch (NumberFormatException failure) {
      return fallback;
    }
  }

  private static String blankToNull(String text) {
    return text == null || text.isBlank() ? null : text.strip();
  }
}
