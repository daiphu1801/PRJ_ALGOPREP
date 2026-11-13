package com.algoprep.bootstrap.identity;

import com.algoprep.identity.application.ports.out.ClassMetricsPort;
import com.algoprep.identity.domain.model.ClassProblemWindow;
import com.algoprep.identity.domain.model.ClassRosterEntry;
import com.algoprep.identity.domain.model.StudentMetrics;
import com.algoprep.judge.application.ports.in.GetClassStudentSubmissionMetricsUseCase;
import com.algoprep.judge.domain.model.ClassMetricsScope;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Type translation only: identity asks for the figures of the students of one class, the judge
 * answers through F4 P1. The read runs on a virtual thread with a 3 second deadline; a failure is
 * one {@link IllegalStateException}, never empty figures.
 */
@Component
public final class ClassMetricsBridge implements ClassMetricsPort {
  private final GetClassStudentSubmissionMetricsUseCase metrics;
  private final Duration limit;

  @Autowired
  public ClassMetricsBridge(GetClassStudentSubmissionMetricsUseCase metrics) {
    this(metrics, Duration.ofSeconds(3));
  }

  ClassMetricsBridge(GetClassStudentSubmissionMetricsUseCase metrics, Duration limit) {
    this.metrics = metrics;
    this.limit = limit;
  }

  @Override
  public List<StudentMetrics> read(
      UUID classId,
      List<ClassRosterEntry> students,
      List<ClassProblemWindow> assignments,
      int weekCount) {
    var scope =
        new ClassMetricsScope(
            classId,
            students.stream()
                .map(s -> new ClassMetricsScope.Student(s.studentId(), s.joinedAt(), null))
                .toList(),
            assignments.stream()
                .map(
                    a ->
                        new ClassMetricsScope.Assignment(
                            a.problemId(), a.assignedAt(), a.removedAt()))
                .toList(),
            weekCount);
    return BridgeDeadline.call("class-metrics-bridge", limit, () -> metrics.read(scope)).stream()
        .map(
            m ->
                new StudentMetrics(
                    m.studentId(),
                    m.lastSubmittedAt(),
                    m.acceptedRatio(),
                    m.streakDays(),
                    m.weeklyTrend().stream()
                        .map(w -> new StudentMetrics.WeekPoint(w.weekStart(), w.acceptedRatio()))
                        .toList(),
                    m.assignmentResults().stream()
                        .map(
                            r ->
                                new StudentMetrics.AssignmentOutcome(
                                    r.problemId(),
                                    r.assignedAt(),
                                    r.bestRatio(),
                                    r.bestVerdict(),
                                    r.accepted()))
                        .toList()))
        .toList();
  }
}
