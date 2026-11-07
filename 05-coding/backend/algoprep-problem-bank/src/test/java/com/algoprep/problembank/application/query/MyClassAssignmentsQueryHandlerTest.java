package com.algoprep.problembank.application.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.algoprep.common.exception.ResourceNotFoundException;
import com.algoprep.common.exception.ServiceUnavailableException;
import com.algoprep.problembank.application.dto.StudentClassViews.ClassProblemsMeta;
import com.algoprep.problembank.application.dto.StudentProblemViews.PageMeta;
import com.algoprep.problembank.application.ports.out.MyClassAssignmentStatePort;
import com.algoprep.problembank.application.ports.out.StudentEnrollmentPort;
import com.algoprep.problembank.domain.model.ClassAssignmentModels.ClassRef;
import com.algoprep.problembank.domain.model.StudentClassModels.AssignmentKey;
import com.algoprep.problembank.domain.model.StudentClassModels.ClassSolveState;
import com.algoprep.problembank.domain.model.StudentClassModels.MyAssignment;
import com.algoprep.problembank.domain.ports.out.StudentClassAssignmentReadRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class MyClassAssignmentsQueryHandlerTest {
  private static final Instant T0 = Instant.parse("2026-11-01T00:00:00Z");

  private final StudentClassAssignmentReadRepository repository =
      mock(StudentClassAssignmentReadRepository.class);
  private final StudentEnrollmentPort enrollment = mock(StudentEnrollmentPort.class);
  private final MyClassAssignmentStatePort states = mock(MyClassAssignmentStatePort.class);
  private final MyClassAssignmentsQueryHandler handler =
      new MyClassAssignmentsQueryHandler(repository, enrollment, states);
  private final UUID user = UUID.randomUUID();
  private final UUID classA = UUID.fromString("00000000-0000-0000-0000-00000000000a");
  private final UUID classB = UUID.fromString("00000000-0000-0000-0000-00000000000b");

  @Test
  void groupsAreEmptyWhenIdentityCannotAnswerOrTheStudentHasNoClass() {
    when(enrollment.activeClassesOf(user)).thenReturn(Optional.empty());
    assertThat(handler.groups(user).groups()).isEmpty();

    when(enrollment.activeClassesOf(user)).thenReturn(Optional.of(List.of()));
    assertThat(handler.groups(user).groups()).isEmpty();

    verifyNoInteractions(repository, states);
  }

  @Test
  void onlyTheThreeNewestGroupsAreKeptNewestFirstAndOnlyTheirRowsAskTheJudge() {
    var classes = new ArrayList<ClassRef>();
    var rows = new ArrayList<MyAssignment>();
    for (int i = 0; i < 4; i++) {
      var id = UUID.randomUUID();
      classes.add(new ClassRef(id, "Class " + i));
      rows.add(row(id, "p" + i, T0.plusSeconds(i * 10L)));
    }
    var older = row(classes.get(3).id(), "old", T0.minusSeconds(5));
    rows.add(older);
    when(enrollment.activeClassesOf(user)).thenReturn(Optional.of(classes));
    when(repository.active(any(), org.mockito.ArgumentMatchers.eq(true))).thenReturn(rows);
    var answer = new HashMap<AssignmentKey, ClassSolveState>();
    rows.forEach(r -> answer.put(key(r), ClassSolveState.SOLVED));
    when(states.read(any())).thenReturn(Optional.of(answer));

    var result = handler.groups(user);

    assertThat(result.groups())
        .extracting("className")
        .containsExactly("Class 3", "Class 2", "Class 1");
    assertThat(result.groups().get(0).items())
        .extracting("title")
        .containsExactly("Title p3", "Title old");
    assertThat(result.groups().get(0).items().get(0).solveState())
        .isEqualTo(ClassSolveState.SOLVED);
    verify(states)
        .read(
            org.mockito.ArgumentMatchers.argThat(
                keys ->
                    keys.size() == 4
                        && keys.stream().noneMatch(k -> k.classId().equals(classes.get(0).id()))));
  }

  @Test
  void aFailingJudgeLeavesTheStateNullInsteadOfGuessingUnsolved() {
    var one = row(classA, "p", T0);
    when(enrollment.activeClassesOf(user))
        .thenReturn(Optional.of(List.of(new ClassRef(classA, "A"))));
    when(repository.active(any(), anyBoolean())).thenReturn(List.of(one));
    when(states.read(any())).thenReturn(Optional.empty());

    var result = handler.groups(user);

    assertThat(result.groups()).hasSize(1);
    assertThat(result.groups().get(0).items().get(0).solveState()).isNull();
  }

  @Test
  void problemsListEveryClassOrderedByClassThenNewestAndKeepWithdrawnRows() {
    var a1 = row(classA, "a1", T0);
    var a2 = row(classA, "a2", T0.plusSeconds(60));
    var gone = new MyAssignment(classB, UUID.randomUUID(), "b1", "Title b1", "Easy", T0, true);
    when(enrollment.activeClassesOf(user))
        .thenReturn(Optional.of(List.of(new ClassRef(classB, "B"), new ClassRef(classA, "A"))));
    when(repository.active(Set.of(classA, classB), false)).thenReturn(List.of(gone, a1, a2));
    when(states.read(any()))
        .thenReturn(
            Optional.of(
                java.util.Map.of(
                    key(a1), ClassSolveState.UNSOLVED,
                    key(a2), ClassSolveState.ATTEMPTED,
                    key(gone), ClassSolveState.SOLVED)));

    var result = handler.problems(user, null, null, null);

    assertThat(result.page()).isEqualTo(new PageMeta(1, 20, 3, 1));
    assertThat(result.meta()).isEqualTo(new ClassProblemsMeta(3, 1));
    verify(states, times(1)).read(any());
    assertThat(result.items()).extracting("code").containsExactly("a2", "a1", "b1");
    assertThat(result.items())
        .extracting("solveState")
        .containsExactly(
            ClassSolveState.ATTEMPTED, ClassSolveState.UNSOLVED, ClassSolveState.SOLVED);
    assertThat(result.items().get(2).withdrawn()).isTrue();
  }

  @Test
  void aClassFilterReadsOnlyThatClass() {
    var a1 = row(classA, "a1", T0);
    when(enrollment.activeClassesOf(user))
        .thenReturn(Optional.of(List.of(new ClassRef(classA, "A"), new ClassRef(classB, "B"))));
    when(repository.active(Set.of(classA), false)).thenReturn(List.of(a1));
    when(states.read(any()))
        .thenReturn(Optional.of(java.util.Map.of(key(a1), ClassSolveState.SOLVED)));

    assertThat(handler.problems(user, classA.toString(), null, null).items()).hasSize(1);
  }

  @Test
  void problemsArePagedWhileTheMetaCountsEveryRowOfTheClass() {
    var rows = new ArrayList<MyAssignment>();
    var answer = new HashMap<AssignmentKey, ClassSolveState>();
    for (int i = 0; i < 45; i++) {
      var r = row(classA, "p" + i, T0.plusSeconds(i));
      rows.add(r);
      answer.put(key(r), i % 3 == 0 ? ClassSolveState.SOLVED : ClassSolveState.UNSOLVED);
    }
    when(enrollment.activeClassesOf(user))
        .thenReturn(Optional.of(List.of(new ClassRef(classA, "A"))));
    when(repository.active(any(), anyBoolean())).thenReturn(rows);
    when(states.read(any())).thenReturn(Optional.of(answer));

    var last = handler.problems(user, null, "3", null);
    var first = handler.problems(user, null, "1", null);

    assertThat(last.page()).isEqualTo(new PageMeta(3, 20, 45, 3));
    assertThat(last.items()).hasSize(5);
    assertThat(last.items().get(0).code()).isEqualTo("p4");
    assertThat(last.meta()).isEqualTo(new ClassProblemsMeta(45, 15));
    assertThat(first.meta()).isEqualTo(last.meta());
    verify(states, times(2))
        .read(org.mockito.ArgumentMatchers.argThat(keys -> keys.size() == 5 || keys.size() == 20));
    verify(states, times(2)).read(org.mockito.ArgumentMatchers.argThat(keys -> keys.size() == 45));
  }

  @Test
  void aFailingWholeClassReadNullsOnlyTheSolvedCountAndAMissingStateDoesToo() {
    var rows = new ArrayList<MyAssignment>();
    var pageAnswer = new HashMap<AssignmentKey, ClassSolveState>();
    for (int i = 0; i < 25; i++) {
      rows.add(row(classA, "p" + i, T0.plusSeconds(i)));
    }
    var newest =
        rows.stream()
            .sorted(java.util.Comparator.comparing(MyAssignment::assignedAt).reversed())
            .limit(20)
            .toList();
    newest.forEach(r -> pageAnswer.put(key(r), ClassSolveState.SOLVED));
    when(enrollment.activeClassesOf(user))
        .thenReturn(Optional.of(List.of(new ClassRef(classA, "A"))));
    when(repository.active(any(), anyBoolean())).thenReturn(rows);
    when(states.read(org.mockito.ArgumentMatchers.argThat(k -> k != null && k.size() == 20)))
        .thenReturn(Optional.of(pageAnswer));
    when(states.read(org.mockito.ArgumentMatchers.argThat(k -> k != null && k.size() == 25)))
        .thenReturn(Optional.empty());

    var result = handler.problems(user, null, null, null);

    assertThat(result.items()).hasSize(20);
    assertThat(result.meta()).isEqualTo(new ClassProblemsMeta(25, null));

    when(states.read(org.mockito.ArgumentMatchers.argThat(k -> k != null && k.size() == 25)))
        .thenReturn(Optional.of(pageAnswer));
    assertThat(handler.problems(user, null, null, null).meta())
        .isEqualTo(new ClassProblemsMeta(25, null));
  }

  @Test
  void badPagingValuesFallBackToTheDefaultsAndAPageBeyondTheEndIsTheLastPage() {
    var rows = new ArrayList<MyAssignment>();
    for (int i = 0; i < 45; i++) {
      rows.add(row(classA, "p" + i, T0.plusSeconds(i)));
    }
    when(enrollment.activeClassesOf(user))
        .thenReturn(Optional.of(List.of(new ClassRef(classA, "A"))));
    when(repository.active(any(), anyBoolean())).thenReturn(rows);
    when(states.read(any())).thenReturn(Optional.of(new HashMap<>()));

    assertThat(handler.problems(user, null, "x", "51").page())
        .isEqualTo(new PageMeta(1, 20, 45, 3));
    assertThat(handler.problems(user, null, "0", "0").page()).isEqualTo(new PageMeta(1, 20, 45, 3));
    assertThat(handler.problems(user, null, "99", "50").page())
        .isEqualTo(new PageMeta(1, 50, 45, 1));
    assertThat(handler.problems(user, null, "99", "10").page())
        .isEqualTo(new PageMeta(5, 10, 45, 5));
  }

  @Test
  void aBadForeignOrLeftClassIdIsTheSame404() {
    when(enrollment.activeClassesOf(user))
        .thenReturn(Optional.of(List.of(new ClassRef(classA, "A"))));

    assertThatThrownBy(() -> handler.problems(user, "nope", null, null))
        .isInstanceOf(ResourceNotFoundException.class);
    assertThatThrownBy(() -> handler.problems(user, classB.toString(), null, null))
        .isInstanceOf(ResourceNotFoundException.class);
    verify(repository, never()).active(any(), anyBoolean());
  }

  @Test
  void problemsAre503WhenIdentityOrTheJudgeCannotAnswer() {
    when(enrollment.activeClassesOf(user)).thenReturn(Optional.empty());
    assertThatThrownBy(() -> handler.problems(user, null, null, null))
        .isInstanceOfSatisfying(
            ServiceUnavailableException.class, e -> assertThat(e.errorCode()).isEqualTo("PRB-058"));

    when(enrollment.activeClassesOf(user))
        .thenReturn(Optional.of(List.of(new ClassRef(classA, "A"))));
    when(repository.active(any(), anyBoolean())).thenReturn(List.of(row(classA, "p", T0)));
    when(states.read(any())).thenReturn(Optional.empty());
    assertThatThrownBy(() -> handler.problems(user, null, null, null))
        .isInstanceOfSatisfying(
            ServiceUnavailableException.class, e -> assertThat(e.errorCode()).isEqualTo("PRB-057"));
  }

  @Test
  void noAssignmentsMeansAnEmptyListWithoutAskingTheJudge() {
    when(enrollment.activeClassesOf(user))
        .thenReturn(Optional.of(List.of(new ClassRef(classA, "A"))));
    when(repository.active(any(), anyBoolean())).thenReturn(List.of());

    var result = handler.problems(user, null, null, null);
    assertThat(result.items()).isEmpty();
    assertThat(result.meta()).isEqualTo(new ClassProblemsMeta(0, 0));
    verifyNoInteractions(states);
  }

  private static MyAssignment row(UUID classId, String code, Instant at) {
    return new MyAssignment(classId, UUID.randomUUID(), code, "Title " + code, "Easy", at, false);
  }

  private static AssignmentKey key(MyAssignment a) {
    return new AssignmentKey(a.classId(), a.problemId(), a.assignedAt());
  }
}
