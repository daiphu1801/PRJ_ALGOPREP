package com.algoprep.aireview.presentation.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.algoprep.aireview.application.ports.in.GetMyRecentSolutionReviewsUseCase;
import com.algoprep.aireview.application.ports.in.GetMySolutionReviewQuickInsightUseCase;
import com.algoprep.aireview.application.ports.in.GetOrCreateSolutionReviewUseCase;
import com.algoprep.aireview.application.ports.in.ListMyReviewedProblemIdsUseCase;
import com.algoprep.aireview.domain.exception.AiInvalidRequestException;
import com.algoprep.aireview.domain.exception.AiInvalidResponseException;
import com.algoprep.aireview.domain.exception.AiRateLimitedException;
import com.algoprep.aireview.domain.exception.AiSubmissionNotFoundException;
import com.algoprep.aireview.domain.model.CallerCategory;
import com.algoprep.aireview.domain.model.RecentSolutionReview;
import com.algoprep.aireview.domain.model.StoredSolutionReview;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

class SolutionReviewControllerTest {
  private static final String JSON =
      "{\"schemaVersion\":\"v1\",\"approachTitle\":\"t\",\"verdict\":\"OPTIMAL\","
          + "\"isApproachOptimal\":true,\"complexity\":{\"actualTime\":\"O(n)\","
          + "\"optimalTime\":null,\"timeExplanation\":\"x\",\"actualSpace\":\"O(1)\","
          + "\"optimalSpace\":null,\"spaceExplanation\":\"y\"},\"strengths\":[\"s\"],"
          + "\"improvements\":[],\"edgeCases\":[],\"followUpTopics\":[\"dp\"],\"codeDiff\":null}";

  private final GetOrCreateSolutionReviewUseCase create =
      mock(GetOrCreateSolutionReviewUseCase.class);
  private final ListMyReviewedProblemIdsUseCase ids = mock(ListMyReviewedProblemIdsUseCase.class);
  private final GetMySolutionReviewQuickInsightUseCase quick =
      mock(GetMySolutionReviewQuickInsightUseCase.class);
  private final GetMyRecentSolutionReviewsUseCase recent =
      mock(GetMyRecentSolutionReviewsUseCase.class);
  private final SolutionReviewController controller =
      new SolutionReviewController(create, ids, quick, recent);
  private final UUID user = UUID.randomUUID();

  private JwtAuthenticationToken auth(String role) {
    Jwt jwt = Jwt.withTokenValue("t").header("alg", "none").subject(user.toString()).build();
    return new JwtAuthenticationToken(jwt, List.of(new SimpleGrantedAuthority(role)));
  }

  @Test
  void createMapsTheStoredJsonToTheFlatResponseAndAlwaysSaysReferenceOnly() {
    UUID submission = UUID.randomUUID();
    var stored =
        new StoredSolutionReview(
            UUID.randomUUID(),
            submission,
            user,
            UUID.randomUUID(),
            "PYTHON",
            "h",
            UUID.randomUUID(),
            JSON,
            4,
            true,
            Instant.parse("2026-11-09T10:00:00Z"));
    when(create.getOrCreate(user, CallerCategory.INSTRUCTOR, submission)).thenReturn(stored);
    var response = controller.solutionReview(auth("ROLE_INSTRUCTOR"), submission.toString());
    assertThat(response.isReferenceOnly()).isTrue();
    assertThat(response.servedFromCache()).isTrue();
    assertThat(response.readabilityScore()).isEqualTo(4);
    assertThat(response.complexity().actualTime()).isEqualTo("O(n)");
    assertThat(response.complexity().optimalTime()).isNull();
    assertThat(response.codeDiff()).isNull();
    assertThat(response.followUpTopics()).containsExactly("dp");
  }

  @Test
  void theCategoryComesFromTheTokenAuthorities() {
    UUID submission = UUID.randomUUID();
    when(create.getOrCreate(any(), any(), any())).thenThrow(new AiSubmissionNotFoundException());
    assertThatThrownBy(() -> controller.solutionReview(auth("ROLE_ADMIN"), submission.toString()))
        .isInstanceOf(AiSubmissionNotFoundException.class);
    verify(create).getOrCreate(user, CallerCategory.ADMIN, submission);
    assertThatThrownBy(() -> controller.solutionReview(auth("ROLE_STUDENT"), submission.toString()))
        .isInstanceOf(AiSubmissionNotFoundException.class);
    verify(create).getOrCreate(user, CallerCategory.STUDENT, submission);
  }

  @Test
  void aNonUuidSubmissionIdIsNotFound() {
    assertThatThrownBy(() -> controller.solutionReview(auth("ROLE_STUDENT"), "abc"))
        .isInstanceOf(AiSubmissionNotFoundException.class);
  }

  @Test
  void problemIdsAndQuickInsight() {
    UUID problem = UUID.randomUUID();
    when(ids.list(user)).thenReturn(List.of(problem));
    assertThat(controller.reviewedProblemIds(auth("ROLE_STUDENT")).problemIds())
        .containsExactly(problem);
    when(quick.get(user, problem)).thenReturn(Optional.empty());
    assertThat(controller.quickInsight(auth("ROLE_STUDENT"), problem.toString()).review()).isNull();
    var row = new RecentSolutionReview(UUID.randomUUID(), UUID.randomUUID(), problem, JSON, null);
    when(quick.get(user, problem)).thenReturn(Optional.of(row));
    var review = controller.quickInsight(auth("ROLE_STUDENT"), problem.toString()).review();
    assertThat(review.timeComplexity()).isEqualTo("O(n)");
    assertThat(review.spaceComplexity()).isEqualTo("O(1)");
    assertThat(review.isReferenceOnly()).isTrue();
  }

  @Test
  void quickInsightNeedsAUuid() {
    for (String bad : new String[] {null, "abc"}) {
      assertThatThrownBy(() -> controller.quickInsight(auth("ROLE_STUDENT"), bad))
          .isInstanceOfSatisfying(
              AiInvalidRequestException.class, e -> assertThat(e.errorCode()).isEqualTo("AIR-V01"));
    }
  }

  @Test
  void recentDefaultsToThreeAndRejectsANonNumber() {
    UUID problem = UUID.randomUUID();
    var row = new RecentSolutionReview(UUID.randomUUID(), UUID.randomUUID(), problem, JSON, null);
    when(recent.recent(user, 3)).thenReturn(List.of(row));
    var items = controller.recentReviews(auth("ROLE_STUDENT"), null).items();
    assertThat(items).hasSize(1);
    assertThat(items.get(0).verdict()).isEqualTo("OPTIMAL");
    assertThat(items.get(0).timeComplexity()).isEqualTo("O(n)");
    assertThat(items.get(0).problemTitle()).isNull();
    assertThatThrownBy(() -> controller.recentReviews(auth("ROLE_STUDENT"), "x"))
        .isInstanceOfSatisfying(
            AiInvalidRequestException.class, e -> assertThat(e.errorCode()).isEqualTo("AIR-V02"));
  }

  @Test
  void adviceAddsRetryAfterAndMaps502() {
    var advice = new SolutionReviewErrorAdvice();
    var limited = advice.rateLimited(AiRateLimitedException.quota(10, 321));
    assertThat(limited.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
    assertThat(limited.getHeaders().getFirst("Retry-After")).isEqualTo("321");
    assertThat(limited.getBody().getProperties())
        .containsEntry("errorCode", "AIR-402")
        .containsKey("details");
    var bad = advice.invalidResponse(new AiInvalidResponseException("x"));
    assertThat(bad.getStatus()).isEqualTo(502);
    assertThat(bad.getProperties()).containsEntry("errorCode", "AIR-502");
  }
}
