package com.algoprep.problembank.application.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.algoprep.common.exception.PermissionDeniedException;
import com.algoprep.problembank.application.ports.out.ClassProblemMetricsPort;
import com.algoprep.problembank.application.ports.out.InstructorClassPort;
import com.algoprep.problembank.domain.model.ClassAssignmentModels.ActiveRow;
import com.algoprep.problembank.domain.model.ClassAssignmentModels.Assignable;
import com.algoprep.problembank.domain.model.ClassAssignmentModels.AssignablePage;
import com.algoprep.problembank.domain.model.ClassAssignmentModels.ClassRef;
import com.algoprep.problembank.domain.model.ClassAssignmentModels.Metric;
import com.algoprep.problembank.domain.ports.out.ClassAssignmentReadRepository;
import com.algoprep.problembank.domain.ports.out.ClassAssignmentRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ClassAssignmentQueryHandlerTest {
  private final ClassAssignmentRepository assignments = mock(ClassAssignmentRepository.class);
  private final ClassAssignmentReadRepository history = mock(ClassAssignmentReadRepository.class);
  private final InstructorClassPort classes = mock(InstructorClassPort.class);
  private final ClassProblemMetricsPort metrics = mock(ClassProblemMetricsPort.class);
  private final ClassAssignmentQueryHandler handler =
      new ClassAssignmentQueryHandler(assignments, history, classes, metrics);
  private final UUID instructor = UUID.randomUUID();
  private final UUID classA = UUID.randomUUID();
  private final UUID classB = UUID.randomUUID();

  private ActiveRow row(UUID problem, UUID clazz, String title, String at) {
    return new ActiveRow(
        problem, clazz, Instant.parse(at), title, List.of(), "easy", "Easy", false);
  }

  private void owns() {
    when(classes.classesOf(instructor))
        .thenReturn(List.of(new ClassRef(classA, "A"), new ClassRef(classB, "B")));
  }

  @Test
  void aProblemInTwoClassesIsOneRowAndTheNewestAssignmentOrdersTheList() {
    owns();
    var shared = UUID.randomUUID();
    var single = UUID.randomUUID();
    when(assignments.active(any(), any()))
        .thenReturn(
            List.of(
                row(shared, classA, "Shared", "2026-01-01T00:00:00Z"),
                row(shared, classB, "Shared", "2026-01-09T00:00:00Z"),
                row(single, classA, "Single", "2026-01-05T00:00:00Z")));
    when(history.findAssignments(any())).thenReturn(List.of());
    when(metrics.read(any(), any(), any(), any()))
        .thenReturn(Optional.of(Map.of(shared, new Metric(4, 25.0))));

    var result = handler.list(instructor, Map.of());

    assertThat(result.items()).hasSize(2);
    assertThat(result.items().get(0).title()).isEqualTo("Shared");
    assertThat(result.items().get(0).assignedClasses()).hasSize(2);
    assertThat(result.items().get(0).submissionCount()).isEqualTo(4);
    assertThat(result.items().get(1).submissionCount()).isZero();
  }

  @Test
  void aClassOfSomeoneElseIsRefusedAndAnUnknownJudgeAnswerLeavesNumbersNull() {
    owns();

    assertThatThrownBy(
            () -> handler.list(instructor, Map.of("classId", UUID.randomUUID().toString())))
        .isInstanceOf(PermissionDeniedException.class);
    assertThatThrownBy(() -> handler.list(instructor, Map.of("classId", "nope")))
        .isInstanceOf(PermissionDeniedException.class);

    var problem = UUID.randomUUID();
    when(assignments.active(any(), any()))
        .thenReturn(List.of(row(problem, classA, "P", "2026-01-01T00:00:00Z")));
    when(history.findAssignments(any())).thenReturn(List.of());
    when(metrics.read(any(), any(), any(), any())).thenReturn(Optional.empty());

    var item = handler.list(instructor, Map.of("classId", classA.toString())).items().get(0);

    assertThat(item.submissionCount()).isNull();
    assertThat(item.acRatePercent()).isNull();
  }

  @Test
  void theSummaryAveragesOnlyTheRatesThatExistAndRoundsToAWholeNumber() {
    owns();
    var one = UUID.randomUUID();
    var two = UUID.randomUUID();
    var three = UUID.randomUUID();
    when(assignments.active(any(), any()))
        .thenReturn(
            List.of(
                row(one, classA, "1", "2026-01-01T00:00:00Z"),
                row(two, classA, "2", "2026-01-01T00:00:00Z"),
                row(three, classB, "3", "2026-01-01T00:00:00Z")));
    when(history.findAssignments(any())).thenReturn(List.of());
    when(metrics.read(any(), any(), any(), any()))
        .thenReturn(
            Optional.of(
                Map.of(
                    one,
                    new Metric(5, 50.0),
                    two,
                    new Metric(5, 75.0),
                    three,
                    new Metric(0, null))));

    var summary = handler.summary(instructor);

    assertThat(summary.assignedProblemCount()).isEqualTo(3);
    assertThat(summary.classCount()).isEqualTo(2);
    assertThat(summary.avgAcRatePercent()).isEqualTo(63);
  }

  @Test
  void theTopListIsEmptyWhenNothingIsAssignedOrTheJudgeIsDown() {
    owns();
    when(assignments.active(any(), any())).thenReturn(List.of());
    assertThat(handler.top(instructor).items()).isEmpty();

    var problem = UUID.randomUUID();
    when(assignments.active(any(), any()))
        .thenReturn(List.of(row(problem, classA, "P", "2026-01-01T00:00:00Z")));
    when(history.findAssignments(any())).thenReturn(List.of());
    when(metrics.read(any(), any(), any(), any())).thenReturn(Optional.empty());
    assertThat(handler.top(instructor).items()).isEmpty();
  }

  @Test
  void assignableNeedsAnOwnedClassAndClampsTheTotalPages() {
    owns();
    assertThatThrownBy(() -> handler.assignable(instructor, UUID.randomUUID(), Map.of()))
        .isInstanceOf(PermissionDeniedException.class);
    var found = new Assignable(UUID.randomUUID(), "C", "T", "easy", "Easy", List.of("Arrays"));
    when(assignments.assignable(
            any(),
            any(),
            org.mockito.ArgumentMatchers.anyInt(),
            org.mockito.ArgumentMatchers.anyInt()))
        .thenReturn(new AssignablePage(List.of(found), 1, 1));

    var result = handler.assignable(instructor, classA, Map.of("page", "9", "q", "x"));

    assertThat(result.items()).hasSize(1);
    assertThat(result.page().totalPages()).isEqualTo(1);
  }
}
