package com.algoprep.judge.application.query;

import com.algoprep.judge.application.dto.AssignedProblemMetricsQuery;
import com.algoprep.judge.application.dto.SubmissionReads.Membership;
import com.algoprep.judge.application.ports.in.GetAssignedProblemMetricsUseCase;
import com.algoprep.judge.application.ports.in.GetMyClassAssignmentStatesUseCase;
import com.algoprep.judge.application.ports.out.ClassScopeReadPort;
import com.algoprep.judge.application.ports.out.CurrentMetricsStudentPort;
import com.algoprep.judge.domain.exception.AssignmentMetricsUnavailableException;
import com.algoprep.judge.domain.model.AssignmentMetricScope;
import com.algoprep.judge.domain.model.AssignmentPeriod;
import com.algoprep.judge.domain.model.AssignmentSolveState;
import com.algoprep.judge.domain.model.ClassProblemMetric;
import com.algoprep.judge.domain.model.InstructorReadWindow;
import com.algoprep.judge.domain.ports.out.AssignmentMetricsRepository;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
class GetAssignmentMetricsQueryHandler
    implements GetAssignedProblemMetricsUseCase, GetMyClassAssignmentStatesUseCase {
  private final ClassScopeReadPort classes;
  private final CurrentMetricsStudentPort principal;
  private final AssignmentMetricsRepository repository;

  GetAssignmentMetricsQueryHandler(
      ClassScopeReadPort classes,
      CurrentMetricsStudentPort principal,
      AssignmentMetricsRepository repository) {
    this.classes = classes;
    this.principal = principal;
    this.repository = repository;
  }

  @Override
  public Map<UUID, ClassProblemMetric> read(AssignedProblemMetricsQuery query) {
    try {
      var members = classes.read(query.instructorId());
      var owned = members.stream().map(Membership::classId).collect(Collectors.toSet());
      if (!owned.containsAll(query.classIds())
          || query.assignments().stream().anyMatch(a -> !owned.contains(a.classId()))) {
        throw new AssignmentMetricsUnavailableException();
      }
      var windows =
          members.stream()
              .filter(m -> m.studentId() != null && m.leftAt() == null)
              .flatMap(
                  m ->
                      query.assignments().stream()
                          .filter(
                              a ->
                                  a.classId().equals(m.classId())
                                      && query.problemIds().contains(a.problemId()))
                          .map(
                              a ->
                                  new InstructorReadWindow(
                                      m.classId(),
                                      m.studentId(),
                                      a.problemId(),
                                      m.joinedAt(),
                                      m.leftAt(),
                                      a.assignedAt(),
                                      a.removedAt())))
              .toList();
      var result =
          repository.problemMetrics(
              new AssignmentMetricScope(windows, query.classIds(), query.problemIds()));
      if (!result.keySet().equals(query.problemIds())) {
        throw new AssignmentMetricsUnavailableException();
      }
      return Map.copyOf(result);
    } catch (RuntimeException failure) {
      throw new AssignmentMetricsUnavailableException();
    }
  }

  @Override
  public Map<AssignmentPeriod.Key, AssignmentSolveState> read(List<AssignmentPeriod> assignments) {
    UUID student = principal.requireStudent();
    var periods = AssignmentPeriod.distinct(assignments);
    try {
      var result = repository.assignmentStates(student, periods);
      var keys = periods.stream().map(AssignmentPeriod::key).collect(Collectors.toSet());
      if (!result.keySet().equals(keys)) {
        throw new AssignmentMetricsUnavailableException();
      }
      return Map.copyOf(result);
    } catch (RuntimeException failure) {
      throw new AssignmentMetricsUnavailableException();
    }
  }
}
