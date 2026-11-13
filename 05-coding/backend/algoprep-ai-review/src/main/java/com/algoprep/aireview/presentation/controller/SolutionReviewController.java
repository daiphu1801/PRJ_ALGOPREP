package com.algoprep.aireview.presentation.controller;

import com.algoprep.aireview.application.ports.in.GetMyRecentSolutionReviewsUseCase;
import com.algoprep.aireview.application.ports.in.GetMySolutionReviewQuickInsightUseCase;
import com.algoprep.aireview.application.ports.in.GetOrCreateSolutionReviewUseCase;
import com.algoprep.aireview.application.ports.in.ListMyReviewedProblemIdsUseCase;
import com.algoprep.aireview.domain.exception.AiInvalidRequestException;
import com.algoprep.aireview.domain.exception.AiSubmissionNotFoundException;
import com.algoprep.aireview.domain.model.CallerCategory;
import com.algoprep.aireview.domain.model.RecentSolutionReview;
import com.algoprep.aireview.domain.model.StoredSolutionReview;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * DD api F5 sections 4 to 7. The caller id always comes from the token; no endpoint accepts a user
 * id. Every authenticated base category may call, ownership is decided by the use case.
 */
@RestController
@RequestMapping("/api/v1/ai")
class SolutionReviewController {
  private static final JsonMapper MAPPER = JsonMapper.builder().build();
  private static final int DEFAULT_LIMIT = 3;

  private final GetOrCreateSolutionReviewUseCase getOrCreate;
  private final ListMyReviewedProblemIdsUseCase problemIds;
  private final GetMySolutionReviewQuickInsightUseCase quickInsight;
  private final GetMyRecentSolutionReviewsUseCase recent;

  SolutionReviewController(
      GetOrCreateSolutionReviewUseCase getOrCreate,
      ListMyReviewedProblemIdsUseCase problemIds,
      GetMySolutionReviewQuickInsightUseCase quickInsight,
      GetMyRecentSolutionReviewsUseCase recent) {
    this.getOrCreate = getOrCreate;
    this.problemIds = problemIds;
    this.quickInsight = quickInsight;
    this.recent = recent;
  }

  record Complexity(
      String actualTime,
      String optimalTime,
      String timeExplanation,
      String actualSpace,
      String optimalSpace,
      String spaceExplanation) {}

  record CodeDiff(String suggestedCode, String explanation) {}

  record ReviewResponse(
      UUID id,
      UUID submissionId,
      UUID problemId,
      String language,
      Instant createdAt,
      boolean servedFromCache,
      boolean isReferenceOnly,
      int readabilityScore,
      String schemaVersion,
      String approachTitle,
      String verdict,
      boolean isApproachOptimal,
      Complexity complexity,
      List<String> strengths,
      List<String> improvements,
      List<String> edgeCases,
      List<String> followUpTopics,
      CodeDiff codeDiff) {}

  record ProblemIdsResponse(List<UUID> problemIds) {}

  record QuickReview(
      UUID submissionId,
      UUID reviewId,
      String timeComplexity,
      String spaceComplexity,
      String verdict,
      Instant createdAt,
      boolean isReferenceOnly) {}

  record QuickInsightResponse(QuickReview review) {}

  record RecentItem(
      UUID reviewId,
      UUID submissionId,
      UUID problemId,
      String problemTitle,
      String verdict,
      String timeComplexity,
      Instant createdAt) {}

  record RecentResponse(List<RecentItem> items) {}

  @PostMapping("/submissions/{submissionId}/solution-review")
  ReviewResponse solutionReview(JwtAuthenticationToken auth, @PathVariable String submissionId) {
    UUID id = uuidOr(submissionId, AiSubmissionNotFoundException::new);
    return toResponse(getOrCreate.getOrCreate(caller(auth), category(auth), id));
  }

  @GetMapping("/me/solution-reviews/problem-ids")
  ProblemIdsResponse reviewedProblemIds(JwtAuthenticationToken auth) {
    return new ProblemIdsResponse(problemIds.list(caller(auth)));
  }

  @GetMapping("/me/solution-reviews/quick-insight")
  QuickInsightResponse quickInsight(
      JwtAuthenticationToken auth, @RequestParam(required = false) String problemId) {
    UUID problem =
        uuidOr(
            problemId,
            () -> new AiInvalidRequestException("AIR-V01", "problemId is missing or not a UUID"));
    return new QuickInsightResponse(
        quickInsight.get(caller(auth), problem).map(SolutionReviewController::quick).orElse(null));
  }

  @GetMapping("/me/solution-reviews/recent")
  RecentResponse recentReviews(
      JwtAuthenticationToken auth, @RequestParam(required = false) String limit) {
    int parsed = DEFAULT_LIMIT;
    if (limit != null) {
      try {
        parsed = Integer.parseInt(limit.trim());
      } catch (NumberFormatException notANumber) {
        throw new AiInvalidRequestException("AIR-V02", "limit must be an integer from 1 to 10");
      }
    }
    List<RecentItem> items = new ArrayList<>();
    for (RecentSolutionReview r : recent.recent(caller(auth), parsed)) {
      JsonNode json = parse(r.resultJson());
      items.add(
          new RecentItem(
              r.reviewId(),
              r.submissionId(),
              r.problemId(),
              r.problemTitle(),
              json.path("verdict").asString(),
              json.path("complexity").path("actualTime").asString(),
              r.createdAt()));
    }
    return new RecentResponse(items);
  }

  private static UUID caller(JwtAuthenticationToken auth) {
    return UUID.fromString(auth.getToken().getSubject());
  }

  private static CallerCategory category(JwtAuthenticationToken auth) {
    var roles = auth.getAuthorities().stream().map(a -> a.getAuthority()).toList();
    if (roles.contains("ROLE_ADMIN")) {
      return CallerCategory.ADMIN;
    }
    return roles.contains("ROLE_INSTRUCTOR") ? CallerCategory.INSTRUCTOR : CallerCategory.STUDENT;
  }

  private static <E extends RuntimeException> UUID uuidOr(
      String value, java.util.function.Supplier<E> failure) {
    try {
      return UUID.fromString(value);
    } catch (RuntimeException invalid) {
      throw failure.get();
    }
  }

  private static JsonNode parse(String json) {
    return MAPPER.readTree(json);
  }

  private static QuickReview quick(RecentSolutionReview r) {
    JsonNode json = parse(r.resultJson());
    return new QuickReview(
        r.submissionId(),
        r.reviewId(),
        json.path("complexity").path("actualTime").asString(),
        json.path("complexity").path("actualSpace").asString(),
        json.path("verdict").asString(),
        r.createdAt(),
        true);
  }

  private static ReviewResponse toResponse(StoredSolutionReview r) {
    JsonNode json = parse(r.resultJson());
    JsonNode cx = json.path("complexity");
    JsonNode diff = json.path("codeDiff");
    return new ReviewResponse(
        r.id(),
        r.submissionId(),
        r.problemId(),
        r.language(),
        r.createdAt(),
        r.servedFromCache(),
        true,
        r.readabilityScore(),
        json.path("schemaVersion").asString(),
        json.path("approachTitle").asString(),
        json.path("verdict").asString(),
        json.path("isApproachOptimal").asBoolean(),
        new Complexity(
            text(cx, "actualTime"),
            text(cx, "optimalTime"),
            text(cx, "timeExplanation"),
            text(cx, "actualSpace"),
            text(cx, "optimalSpace"),
            text(cx, "spaceExplanation")),
        strings(json, "strengths"),
        strings(json, "improvements"),
        strings(json, "edgeCases"),
        strings(json, "followUpTopics"),
        diff.isObject()
            ? new CodeDiff(text(diff, "suggestedCode"), text(diff, "explanation"))
            : null);
  }

  private static String text(JsonNode node, String key) {
    JsonNode value = node.get(key);
    return value == null || value.isNull() ? null : value.asString();
  }

  private static List<String> strings(JsonNode node, String key) {
    List<String> out = new ArrayList<>();
    node.path(key).forEach(item -> out.add(item.asString()));
    return out;
  }
}
