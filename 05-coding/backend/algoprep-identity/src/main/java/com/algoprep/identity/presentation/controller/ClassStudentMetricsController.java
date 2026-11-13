package com.algoprep.identity.presentation.controller;

import com.algoprep.identity.application.ports.in.GetClassProgressUseCase;
import com.algoprep.identity.application.ports.in.GetClassStudentProfileUseCase;
import com.algoprep.identity.domain.model.ClassMetricViews.ClassProgress;
import com.algoprep.identity.domain.model.ClassStudentRow;
import com.algoprep.identity.domain.model.ClassStudentStatus;
import com.algoprep.identity.domain.model.StudentMetrics;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Class metric reads on one class: #28 and #29 (DD api F1 sections 0.4.2, 0.8.2). Both need {@code
 * CLASS_MANAGEMENT:READ}; a class or student id that is malformed, missing or foreign is 404
 * IDT-414.
 */
@RestController
@RequestMapping("/api/v1/identity/classes/{classId}")
class ClassStudentMetricsController {
  private final GetClassStudentProfileUseCase profile;
  private final GetClassProgressUseCase progress;

  ClassStudentMetricsController(
      GetClassStudentProfileUseCase profile, GetClassProgressUseCase progress) {
    this.profile = profile;
    this.progress = progress;
  }

  record AssignmentProgress(UUID problemId, BigDecimal bestRatio, String bestVerdict) {}

  record ProfileResponse(
      UUID classId,
      UUID studentId,
      String displayName,
      String email,
      String phone,
      String className,
      String joinedAt,
      String status,
      List<AssignmentProgress> assignmentProgress) {}

  record ProgressResponse(
      UUID classId,
      String className,
      int studentCount,
      Integer completionPercent,
      Double avgScore,
      Map<String, Integer> statusCounts) {}

  @GetMapping("/students/{studentId}/detail")
  @PreAuthorize("hasAuthority('CLASS_MANAGEMENT:READ')")
  ProfileResponse profile(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable String classId,
      @PathVariable String studentId) {
    ClassStudentRow row =
        profile.get(
            UUID.fromString(jwt.getSubject()), ClassPath.id(classId), ClassPath.id(studentId));
    List<AssignmentProgress> assigned =
        row.metrics().assignments().stream()
            .sorted(
                Comparator.comparing(
                        StudentMetrics.AssignmentOutcome::assignedAt, Comparator.reverseOrder())
                    .thenComparing(StudentMetrics.AssignmentOutcome::problemId))
            .map(a -> new AssignmentProgress(a.problemId(), a.bestRatio(), a.bestVerdict()))
            .toList();
    return new ProfileResponse(
        row.classId(),
        row.student().studentId(),
        row.student().displayName(),
        row.student().email(),
        row.student().phone(),
        row.className(),
        row.student().joinedAt().toString(),
        row.status().name(),
        assigned);
  }

  @GetMapping("/progress")
  @PreAuthorize("hasAuthority('CLASS_MANAGEMENT:READ')")
  ProgressResponse progress(@AuthenticationPrincipal Jwt jwt, @PathVariable String classId) {
    ClassProgress p = progress.get(UUID.fromString(jwt.getSubject()), ClassPath.id(classId));
    Map<String, Integer> counts = new LinkedHashMap<>();
    for (ClassStudentStatus s : ClassStudentStatus.values()) {
      counts.put(s.name(), p.statusCounts().get(s));
    }
    return new ProgressResponse(
        p.classId(), p.className(), p.studentCount(), p.completionPercent(), p.avgScore(), counts);
  }
}
