package com.algoprep.judge.application.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.algoprep.judge.application.dto.AssignedProblemMetricsQuery;
import com.algoprep.judge.application.dto.SubmissionReads.Membership;
import com.algoprep.judge.application.ports.out.ClassScopeReadPort;
import com.algoprep.judge.application.ports.out.CurrentMetricsStudentPort;
import com.algoprep.judge.domain.exception.AssignmentMetricsUnavailableException;
import com.algoprep.judge.domain.model.AssignmentMetricScope;
import com.algoprep.judge.domain.model.AssignmentPeriod;
import com.algoprep.judge.domain.model.AssignmentSolveState;
import com.algoprep.judge.domain.model.ClassProblemMetric;
import com.algoprep.judge.domain.ports.out.AssignmentMetricsRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.access.AccessDeniedException;

class GetAssignmentMetricsQueryHandlerTest {
  private final ClassScopeReadPort classes = mock(ClassScopeReadPort.class);
  private final CurrentMetricsStudentPort principal = mock(CurrentMetricsStudentPort.class);
  private final AssignmentMetricsRepository repository = mock(AssignmentMetricsRepository.class);
  private final GetAssignmentMetricsQueryHandler handler =
      new GetAssignmentMetricsQueryHandler(classes, principal, repository);
  private final UUID instructor = UUID.randomUUID();
  private final UUID student = UUID.randomUUID();
  private final UUID classA = UUID.randomUUID();
  private final UUID classB = UUID.randomUUID();
  private final UUID problem = UUID.randomUUID();
  private final Instant start = Instant.parse("2026-10-01T00:00:00Z");

  private AssignmentPeriod period(UUID classId) {
    return new AssignmentPeriod(classId, problem, start, null);
  }

  private AssignedProblemMetricsQuery query(Set<UUID> selected, List<AssignmentPeriod> periods) {
    return new AssignedProblemMetricsQuery(instructor, selected, Set.of(problem), periods);
  }

  @Test
  void p6KeepsEveryOwnedCandidateBeforeTabFilteringButRemovesFormerMembers() {
    when(classes.read(instructor))
        .thenReturn(
            List.of(
                new Membership(classA, "A", student, "S", start, null),
                new Membership(classB, "B", student, "S", start, null),
                new Membership(
                    classA, "A", UUID.randomUUID(), "Former", start, start.plusSeconds(100)),
                new Membership(classB, "B", null, null, null, null)));
    when(repository.problemMetrics(any()))
        .thenReturn(Map.of(problem, new ClassProblemMetric(0, null)));
    assertThat(handler.read(query(Set.of(classA), List.of(period(classA), period(classB)))))
        .containsEntry(problem, new ClassProblemMetric(0, null));
    var scope = ArgumentCaptor.forClass(AssignmentMetricScope.class);
    verify(repository).problemMetrics(scope.capture());
    assertThat(scope.getValue().windows())
        .hasSize(2)
        .allSatisfy(w -> assertThat(w.studentId()).isEqualTo(student));
    assertThat(scope.getValue().windows().stream().map(w -> w.classId()))
        .containsExactly(classA, classB);
    assertThat(scope.getValue().classIds()).containsExactly(classA);
    verifyNoInteractions(principal);
  }

  @Test
  void p6RejectsForeignTabsAndForeignAssignmentsEvenWhenNotSelected() {
    when(classes.read(instructor))
        .thenReturn(List.of(new Membership(classA, "A", null, null, null, null)));
    assertThatThrownBy(() -> handler.read(query(Set.of(classB), List.of())))
        .isInstanceOf(AssignmentMetricsUnavailableException.class)
        .hasNoCause();
    assertThatThrownBy(() -> handler.read(query(Set.of(classA), List.of(period(classB)))))
        .isInstanceOf(AssignmentMetricsUnavailableException.class)
        .hasNoCause();
    verifyNoInteractions(repository);
  }

  @Test
  void p6ScopeAndDatabaseFailuresCannotProduceFalseZeroMetrics() {
    when(classes.read(instructor)).thenThrow(new IllegalStateException("private F1 SQL"));
    assertThatThrownBy(() -> handler.read(query(Set.of(), List.of())))
        .isInstanceOf(AssignmentMetricsUnavailableException.class)
        .hasMessage("Assignment metrics unavailable")
        .hasNoCause();
    doReturn(List.of()).when(classes).read(instructor);
    when(repository.problemMetrics(any())).thenReturn(Map.of());
    assertThatThrownBy(() -> handler.read(query(Set.of(), List.of())))
        .isInstanceOf(AssignmentMetricsUnavailableException.class);
    when(repository.problemMetrics(any())).thenThrow(new IllegalStateException("private database"));
    assertThatThrownBy(() -> handler.read(query(Set.of(), List.of())))
        .isInstanceOf(AssignmentMetricsUnavailableException.class)
        .hasNoCause();
  }

  @Test
  void p7UsesOnlyCurrentStudentAndSuppliedPeriodsWithoutReadingOtherModules() {
    var period = period(classA);
    when(principal.requireStudent()).thenReturn(student);
    when(repository.assignmentStates(student, List.of(period)))
        .thenReturn(Map.of(period.key(), AssignmentSolveState.ATTEMPTED));
    var result = handler.read(List.of(period, period));
    assertThat(result).containsExactly(Map.entry(period.key(), AssignmentSolveState.ATTEMPTED));
    assertThatThrownBy(() -> result.clear()).isInstanceOf(UnsupportedOperationException.class);
    verify(repository).assignmentStates(student, List.of(period));
    verifyNoInteractions(classes);
  }

  @Test
  void p7DeniesBeforeQueryAndDoesNotMaskAnAccessDenial() {
    when(principal.requireStudent()).thenThrow(new AccessDeniedException("student required"));
    assertThatThrownBy(() -> handler.read(List.of(period(classA))))
        .isInstanceOf(AccessDeniedException.class);
    verifyNoInteractions(classes, repository);
  }

  @Test
  void p7MissingKeysOrDependencyFailuresRemainFailures() {
    var period = period(classA);
    when(principal.requireStudent()).thenReturn(student);
    when(repository.assignmentStates(student, List.of(period))).thenReturn(Map.of());
    assertThatThrownBy(() -> handler.read(List.of(period)))
        .isInstanceOf(AssignmentMetricsUnavailableException.class)
        .hasNoCause();
    when(repository.assignmentStates(student, List.of(period)))
        .thenThrow(new IllegalStateException("private SQL credentials"));
    assertThatThrownBy(() -> handler.read(List.of(period)))
        .isInstanceOf(AssignmentMetricsUnavailableException.class)
        .hasMessage("Assignment metrics unavailable")
        .hasNoCause();
  }
}
