package com.algoprep.identity.presentation.controller;

import com.algoprep.identity.application.ports.in.GetMyActivityCalendarUseCase;
import com.algoprep.identity.application.ports.in.GetMyProgressSummaryUseCase;
import com.algoprep.identity.application.ports.in.ListMyProblemSolveStatesUseCase;
import com.algoprep.identity.application.ports.in.ListMyRecentActivityUseCase;
import com.algoprep.identity.domain.model.ActivityCalendar;
import com.algoprep.identity.domain.model.MyProgressSummary;
import com.algoprep.identity.domain.model.SolveState;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Personal progress reads #46, #47, #48 and #55. The caller comes from the token; no endpoint
 * accepts another user's id.
 */
@RestController
@RequestMapping("/api/v1/identity/me")
class MyProgressController {
  private final GetMyProgressSummaryUseCase summary;
  private final ListMyRecentActivityUseCase activity;
  private final GetMyActivityCalendarUseCase calendar;
  private final ListMyProblemSolveStatesUseCase solveStates;

  MyProgressController(
      GetMyProgressSummaryUseCase summary,
      ListMyRecentActivityUseCase activity,
      GetMyActivityCalendarUseCase calendar,
      ListMyProblemSolveStatesUseCase solveStates) {
    this.summary = summary;
    this.activity = activity;
    this.calendar = calendar;
    this.solveStates = solveStates;
  }

  record ActivityItem(String activityType, String refType, UUID refId, String occurredAt) {}

  record ActivityResponse(List<ActivityItem> items) {}

  record DayCount(String day, int submissionCount) {}

  record CalendarResponse(String from, String to, List<DayCount> days) {}

  record SolveStatesResponse(Map<UUID, SolveState> states) {}

  @GetMapping("/progress/summary")
  MyProgressSummary summary(@AuthenticationPrincipal Jwt jwt) {
    return summary.get(UUID.fromString(jwt.getSubject()));
  }

  @GetMapping("/recent-activity")
  ActivityResponse recentActivity(@AuthenticationPrincipal Jwt jwt) {
    return new ActivityResponse(
        activity.list(UUID.fromString(jwt.getSubject())).stream()
            .map(
                e ->
                    new ActivityItem(
                        e.activityType(), e.refType(), e.refId(), e.occurredAt().toString()))
            .toList());
  }

  @GetMapping("/activity-calendar")
  CalendarResponse activityCalendar(@AuthenticationPrincipal Jwt jwt) {
    ActivityCalendar c = calendar.get(UUID.fromString(jwt.getSubject()));
    return new CalendarResponse(
        c.from().toString(),
        c.to().toString(),
        c.days().stream().map(d -> new DayCount(d.day().toString(), d.submissionCount())).toList());
  }

  @GetMapping("/problem-solve-states")
  SolveStatesResponse problemSolveStates(
      @AuthenticationPrincipal Jwt jwt, @RequestParam(required = false) String problemIds) {
    return new SolveStatesResponse(solveStates.list(UUID.fromString(jwt.getSubject()), problemIds));
  }
}
