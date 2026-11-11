package com.algoprep.aireview.presentation.controller;

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
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * DD api F5 sections 27 to 31 (ADM0302 usage statistics and anomaly alerts). Reads need {@code
 * AI_TOKEN_BUDGET:READ}, marking an alert reviewed needs {@code AI_TOKEN_BUDGET:UPDATE}, each
 * checked on its own; {@code AI_CONFIG} is not enough. All need the ADMIN category.
 */
@RestController
@RequestMapping("/api/v1/ai/admin/usage")
class AiUsageController {
  private static final String READ =
      "hasAuthority('ROLE_ADMIN') and hasAuthority('AI_TOKEN_BUDGET:READ')";
  private static final String UPDATE =
      "hasAuthority('ROLE_ADMIN') and hasAuthority('AI_TOKEN_BUDGET:UPDATE')";

  private final GetAiUsageDailyUseCase daily;
  private final ListAiUsageAlertsUseCase alerts;
  private final MarkAiUsageAlertReviewedUseCase review;
  private final ListTopAiUsersUseCase topUsers;
  private final ListTopAiProblemsUseCase topProblems;

  AiUsageController(
      GetAiUsageDailyUseCase daily,
      ListAiUsageAlertsUseCase alerts,
      MarkAiUsageAlertReviewedUseCase review,
      ListTopAiUsersUseCase topUsers,
      ListTopAiProblemsUseCase topProblems) {
    this.daily = daily;
    this.alerts = alerts;
    this.review = review;
    this.topUsers = topUsers;
    this.topProblems = topProblems;
  }

  record Reviewed(UUID alertId, String status, Instant reviewedAt, boolean alreadyReviewed) {}

  record Alerts(List<UsageAlertItem> items) {}

  record TopUsers(List<TopUserItem> items) {}

  record TopProblems(List<TopProblemItem> items) {}

  @GetMapping("/daily")
  @PreAuthorize(READ)
  UsageDaily daily(@RequestParam(required = false) String rangeDays) {
    return daily.daily(UsageRequests.rangeDays(rangeDays));
  }

  @GetMapping("/alerts")
  @PreAuthorize(READ)
  Alerts alerts() {
    return new Alerts(alerts.list());
  }

  @PostMapping("/alerts/{alertId}/review")
  @PreAuthorize(UPDATE)
  Reviewed review(@PathVariable String alertId) {
    AlertReview result = review.review(UsageRequests.alertId(alertId));
    return new Reviewed(
        result.alertId(), "REVIEWED", result.reviewedAt(), result.alreadyReviewed());
  }

  @GetMapping("/top-users")
  @PreAuthorize(READ)
  TopUsers topUsers(@RequestParam(required = false) String limit) {
    return new TopUsers(topUsers.topUsers(UsageRequests.limit(limit)));
  }

  @GetMapping("/top-problems")
  @PreAuthorize(READ)
  TopProblems topProblems(@RequestParam(required = false) String limit) {
    return new TopProblems(topProblems.topProblems(UsageRequests.limit(limit)));
  }
}
