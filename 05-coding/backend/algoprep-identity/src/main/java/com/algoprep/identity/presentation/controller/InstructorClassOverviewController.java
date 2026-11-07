package com.algoprep.identity.presentation.controller;

import com.algoprep.identity.application.dto.InstructorStudentsQuery;
import com.algoprep.identity.application.ports.in.GetClassScoreTrendUseCase;
import com.algoprep.identity.application.ports.in.ListClassAttentionStudentsUseCase;
import com.algoprep.identity.application.ports.in.ListClassesSummaryUseCase;
import com.algoprep.identity.application.ports.in.ListInstructorStudentsUseCase;
import com.algoprep.identity.domain.model.ClassMetricViews.AttentionStudent;
import com.algoprep.identity.domain.model.ClassMetricViews.ClassCard;
import com.algoprep.identity.domain.model.ClassMetricViews.ClassesSummary;
import com.algoprep.identity.domain.model.ClassMetricViews.ScoreTrend;
import com.algoprep.identity.domain.model.ClassMetricViews.StudentPage;
import com.algoprep.identity.domain.model.ClassProgressRules;
import com.algoprep.identity.domain.model.ClassStudentRow;
import com.algoprep.identity.domain.model.ScheduleSpec;
import com.algoprep.identity.domain.model.StudentMetrics;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Class metric reads across the classes of the instructor: #56 to #59 (DD api F1 sections 0.8.2,
 * 0.8.3). All need {@code CLASS_MANAGEMENT:READ}; a {@code classId} that is malformed, missing or
 * foreign is 404 IDT-414.
 */
@RestController
@RequestMapping("/api/v1/identity/instructor")
class InstructorClassOverviewController {
  private final ListClassesSummaryUseCase summary;
  private final GetClassScoreTrendUseCase trend;
  private final ListClassAttentionStudentsUseCase attention;
  private final ListInstructorStudentsUseCase students;

  InstructorClassOverviewController(
      ListClassesSummaryUseCase summary,
      GetClassScoreTrendUseCase trend,
      ListClassAttentionStudentsUseCase attention,
      ListInstructorStudentsUseCase students) {
    this.summary = summary;
    this.trend = trend;
    this.attention = attention;
    this.students = students;
  }

  record CardResponse(
      UUID classId,
      String name,
      String description,
      ScheduleSpec schedule,
      int studentCount,
      Integer completionPercent,
      Double avgScore,
      int absentCount,
      String createdAt) {}

  record SummaryResponse(
      int classCount,
      int studentCount,
      Integer avgCompletionPercent,
      int absentStudentCount,
      List<CardResponse> items) {}

  record PointResponse(String weekStart, Double avgScore) {}

  record TrendResponse(UUID classId, String className, List<PointResponse> points) {}

  record AttentionResponse(List<AttentionStudent> items) {}

  /** {@code trendPoints} holds the weekly scores of the student, null for a week without one. */
  record StudentRow(
      UUID classId,
      String className,
      UUID studentId,
      String displayName,
      String joinedAt,
      int completionPercent,
      Double avgScore,
      String status,
      int streakDays,
      List<Double> trendPoints,
      String lastSubmittedAt) {}

  record PageMeta(int page, int pageSize, long totalCount, int totalPages) {}

  record StudentPageResponse(List<StudentRow> items, PageMeta page) {}

  @GetMapping("/classes/summary")
  @PreAuthorize("hasAuthority('CLASS_MANAGEMENT:READ')")
  SummaryResponse summary(@AuthenticationPrincipal Jwt jwt) {
    ClassesSummary s = summary.summary(UUID.fromString(jwt.getSubject()));
    return new SummaryResponse(
        s.classCount(),
        s.studentCount(),
        s.avgCompletionPercent(),
        s.absentStudentCount(),
        s.items().stream().map(InstructorClassOverviewController::card).toList());
  }

  private static CardResponse card(ClassCard c) {
    return new CardResponse(
        c.owned().id(),
        c.owned().name(),
        c.owned().description(),
        c.owned().schedule(),
        c.owned().studentCount(),
        c.completionPercent(),
        c.avgScore(),
        c.absentCount(),
        c.owned().createdAt().toString());
  }

  @GetMapping("/classes/score-trend")
  @PreAuthorize("hasAuthority('CLASS_MANAGEMENT:READ')")
  TrendResponse scoreTrend(
      @AuthenticationPrincipal Jwt jwt, @RequestParam(required = false) String classId) {
    ScoreTrend t =
        trend.trend(
            UUID.fromString(jwt.getSubject()), classId == null ? null : ClassPath.id(classId));
    return new TrendResponse(
        t.classId(),
        t.className(),
        t.points().stream()
            .map(p -> new PointResponse(p.weekStart().toString(), p.avgScore()))
            .toList());
  }

  @GetMapping("/classes/attention-students")
  @PreAuthorize("hasAuthority('CLASS_MANAGEMENT:READ')")
  AttentionResponse attention(
      @AuthenticationPrincipal Jwt jwt, @RequestParam(required = false) String classId) {
    return new AttentionResponse(
        attention.list(
            UUID.fromString(jwt.getSubject()), classId == null ? null : ClassPath.id(classId)));
  }

  @GetMapping("/students")
  @PreAuthorize("hasAuthority('CLASS_MANAGEMENT:READ')")
  StudentPageResponse students(
      @AuthenticationPrincipal Jwt jwt,
      @RequestParam(required = false) String classId,
      @RequestParam(name = "q", required = false) String keyword,
      @RequestParam(required = false) Integer page,
      @RequestParam(required = false) Integer pageSize) {
    StudentPage result =
        students.list(
            new InstructorStudentsQuery(
                UUID.fromString(jwt.getSubject()),
                classId == null ? null : ClassPath.id(classId),
                keyword,
                page,
                pageSize));
    return new StudentPageResponse(
        result.items().stream().map(InstructorClassOverviewController::row).toList(),
        new PageMeta(result.page(), result.pageSize(), result.totalCount(), result.totalPages()));
  }

  private static StudentRow row(ClassStudentRow r) {
    StudentMetrics m = r.metrics();
    return new StudentRow(
        r.classId(),
        r.className(),
        r.student().studentId(),
        r.student().displayName(),
        r.student().joinedAt().toString(),
        r.completionPercent(),
        r.avgScore(),
        r.status().name(),
        m.streakDays(),
        ClassProgressRules.weeklyScores(List.of(r)),
        m.lastSubmittedAt() == null ? null : m.lastSubmittedAt().toString());
  }
}
