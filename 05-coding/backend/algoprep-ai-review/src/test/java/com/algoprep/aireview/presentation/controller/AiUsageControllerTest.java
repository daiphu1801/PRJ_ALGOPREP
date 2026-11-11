package com.algoprep.aireview.presentation.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.algoprep.aireview.application.dto.TopProblemItem;
import com.algoprep.aireview.application.dto.TopUserItem;
import com.algoprep.aireview.application.dto.UsageAlertItem;
import com.algoprep.aireview.application.dto.UsageDaily;
import com.algoprep.aireview.application.ports.in.GetAiUsageDailyUseCase;
import com.algoprep.aireview.application.ports.in.ListAiUsageAlertsUseCase;
import com.algoprep.aireview.application.ports.in.ListTopAiProblemsUseCase;
import com.algoprep.aireview.application.ports.in.ListTopAiUsersUseCase;
import com.algoprep.aireview.application.ports.in.MarkAiUsageAlertReviewedUseCase;
import com.algoprep.aireview.domain.model.AlertReview;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

class AiUsageControllerTest {
  private final GetAiUsageDailyUseCase daily = mock(GetAiUsageDailyUseCase.class);
  private final ListAiUsageAlertsUseCase alerts = mock(ListAiUsageAlertsUseCase.class);
  private final MarkAiUsageAlertReviewedUseCase review =
      mock(MarkAiUsageAlertReviewedUseCase.class);
  private final ListTopAiUsersUseCase topUsers = mock(ListTopAiUsersUseCase.class);
  private final ListTopAiProblemsUseCase topProblems = mock(ListTopAiProblemsUseCase.class);
  private final AiUsageController controller =
      new AiUsageController(daily, alerts, review, topUsers, topProblems);

  private static String rule(String method) {
    for (Method m : AiUsageController.class.getDeclaredMethods()) {
      if (m.getName().equals(method) && m.isAnnotationPresent(PreAuthorize.class)) {
        return m.getAnnotation(PreAuthorize.class).value();
      }
    }
    throw new AssertionError("no rule for " + method);
  }

  @Test
  void theFourReadsNeedTheBudgetReadCellAndNeverTheConfigOne() {
    for (String method : new String[] {"daily", "alerts", "topUsers", "topProblems"}) {
      assertThat(rule(method))
          .contains("ROLE_ADMIN", "AI_TOKEN_BUDGET:READ")
          .doesNotContain("AI_CONFIG", "UPDATE");
    }
  }

  @Test
  void markingReviewedNeedsTheBudgetUpdateCellOnItsOwn() {
    assertThat(rule("review"))
        .contains("ROLE_ADMIN", "AI_TOKEN_BUDGET:UPDATE")
        .doesNotContain("AI_CONFIG", "READ");
  }

  @Test
  void dailyDefaultsToFourteenAndPassesTheChosenRange() {
    when(daily.daily(14)).thenReturn(new UsageDaily(14, List.of()));
    assertThat(controller.daily(null).rangeDays()).isEqualTo(14);
    controller.daily("30");
    verify(daily).daily(30);
  }

  @Test
  void reviewAnswersReviewedWithTheStoredTimeAndFlag() {
    UUID id = UUID.randomUUID();
    Instant at = Instant.parse("2026-11-10T05:00:00Z");
    when(review.review(id)).thenReturn(new AlertReview(id, at, true));
    AiUsageController.Reviewed body = controller.review(id.toString());
    assertThat(body.status()).isEqualTo("REVIEWED");
    assertThat(body.reviewedAt()).isEqualTo(at);
    assertThat(body.alreadyReviewed()).isTrue();
  }

  @Test
  void alertsAreWrappedInItems() {
    UsageAlertItem item =
        new UsageAlertItem(
            UsageAlertItem.Type.BUDGET,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            BigDecimal.TEN);
    when(alerts.list()).thenReturn(List.of(item));
    assertThat(controller.alerts().items()).containsExactly(item);
  }

  @Test
  void topListsDefaultToFive() {
    UUID user = UUID.randomUUID();
    when(topUsers.topUsers(5)).thenReturn(List.of(new TopUserItem(user, "An", 9, 2, null)));
    TopUserItem row = controller.topUsers(null).items().get(0);
    assertThat(row.displayName()).isEqualTo("An");
    TopProblemItem problem = new TopProblemItem(UUID.randomUUID(), null, null, null, 9, 3, 3);
    when(topProblems.topProblems(7)).thenReturn(List.of(problem));
    assertThat(controller.topProblems("7").items()).containsExactly(problem);
  }
}
