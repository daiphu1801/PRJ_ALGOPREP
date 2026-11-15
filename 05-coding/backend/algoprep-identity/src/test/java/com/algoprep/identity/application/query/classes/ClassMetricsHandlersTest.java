package com.algoprep.identity.application.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.algoprep.common.exception.AlgoPrepException;
import com.algoprep.identity.application.dto.InstructorStudentsQuery;
import com.algoprep.identity.application.ports.out.ClassAssignmentLookupPort;
import com.algoprep.identity.application.ports.out.ClassMetricsPort;
import com.algoprep.identity.domain.model.ClassProblemWindow;
import com.algoprep.identity.domain.model.ClassRosterEntry;
import com.algoprep.identity.domain.model.ClassStudentStatus;
import com.algoprep.identity.domain.model.InstructorClass;
import com.algoprep.identity.domain.model.StudentMetrics;
import com.algoprep.identity.domain.model.StudentMetrics.AssignmentOutcome;
import com.algoprep.identity.domain.model.StudentMetrics.WeekPoint;
import com.algoprep.identity.domain.ports.out.InstructorClassRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** The six class metric reads and the roster, over mocked ports and a fixed clock. */
class ClassMetricsHandlersTest {
  private static final Instant NOW = Instant.parse("2026-11-07T05:00:00Z");

  private final InstructorClassRepository classes = mock(InstructorClassRepository.class);
  private final ClassAssignmentLookupPort lookup = mock(ClassAssignmentLookupPort.class);
  private final ClassMetricsPort metrics = mock(ClassMetricsPort.class);
  private final ClassMetricsAssembler assembler =
      new ClassMetricsAssembler(classes, lookup, metrics, () -> NOW);
  private final UUID instructor = UUID.randomUUID();
  private final UUID classA = UUID.randomUUID();
  private final UUID classB = UUID.randomUUID();
  private final UUID problem = UUID.randomUUID();
  private final ClassRosterEntry an = student("An");
  private final ClassRosterEntry binh = student("binh");
  private final ClassRosterEntry chi = student("Chi");

  private static ClassRosterEntry student(String name) {
    return new ClassRosterEntry(
        UUID.randomUUID(), name, name + "@x.test", null, NOW.minusSeconds(86_400L * 90));
  }

  private static List<WeekPoint> weeks(Double... ratios) {
    return java.util.stream.IntStream.range(0, ratios.length)
        .mapToObj(i -> new WeekPoint(LocalDate.of(2026, 9, 21).plusWeeks(i), ratios[i]))
        .toList();
  }

  private StudentMetrics figures(ClassRosterEntry s, Instant last, Double ratio, boolean accepted) {
    return new StudentMetrics(
        s.studentId(),
        last,
        ratio,
        2,
        weeks(null, null, null, null, 0.5, ratio),
        List.of(
            new AssignmentOutcome(
                problem, NOW.minusSeconds(86_400L * 30), BigDecimal.ONE, "ACCEPTED", accepted)));
  }

  @BeforeEach
  void world() {
    InstructorClass a =
        new InstructorClass(classA, "Class A", null, null, 2, NOW.minusSeconds(100));
    InstructorClass b = new InstructorClass(classB, "Class B", "d", null, 1, NOW.minusSeconds(200));
    when(classes.listOwned(instructor)).thenReturn(List.of(a, b));
    when(classes.findOwned(classA, instructor)).thenReturn(Optional.of(a));
    when(classes.findOwned(classB, instructor)).thenReturn(Optional.of(b));
    when(classes.listActiveRoster(classA)).thenReturn(List.of(an, binh));
    when(classes.listActiveRoster(classB)).thenReturn(List.of(an));
    when(lookup.assignmentsOf(any()))
        .thenReturn(
            List.of(
                new ClassProblemWindow(classA, problem, NOW.minusSeconds(86_400L * 30), null),
                new ClassProblemWindow(classB, problem, NOW.minusSeconds(86_400L * 30), null)));
    // An: recent, accepted. Binh: silent for ten days.
    when(metrics.read(any(), any(), any(), anyInt()))
        .thenAnswer(
            call -> {
              List<ClassRosterEntry> asked = call.getArgument(1);
              return asked.stream()
                  .map(
                      s ->
                          s == an
                              ? figures(s, NOW.minusSeconds(3600), 1.0, true)
                              : figures(s, NOW.minusSeconds(10 * 86_400L), 0.5, false))
                  .toList();
            });
  }

  private static void assertCode(Throwable error, String code) {
    assertThat(error)
        .isInstanceOfSatisfying(
            AlgoPrepException.class, e -> assertThat(e.errorCode()).isEqualTo(code));
  }

  @Test
  void profileGivesTheStudentFiguresAndNotFoundForForeignClassOrStudent() {
    var handler = new GetClassStudentProfileQueryHandler(classes, assembler);

    var row = handler.get(instructor, classA, binh.studentId());

    assertThat(row.status()).isEqualTo(ClassStudentStatus.ABSENT);
    assertThat(row.className()).isEqualTo("Class A");
    assertThatThrownBy(() -> handler.get(UUID.randomUUID(), classA, binh.studentId()))
        .satisfies(e -> assertCode(e, "IDT-414"));
    assertThatThrownBy(() -> handler.get(instructor, classA, chi.studentId()))
        .satisfies(e -> assertCode(e, "IDT-414"));
  }

  @Test
  void profileIs503WhenTheJudgeFailsAndNeverZeros() {
    doThrow(new IllegalStateException("x")).when(metrics).read(any(), any(), any(), anyInt());

    assertThatThrownBy(
            () ->
                new GetClassStudentProfileQueryHandler(classes, assembler)
                    .get(instructor, classA, an.studentId()))
        .satisfies(e -> assertCode(e, "IDT-507"));
  }

  @Test
  void aMissingStudentInTheJudgeAnswerIs503() {
    doReturn(List.of()).when(metrics).read(any(), any(), any(), anyInt());

    assertThatThrownBy(
            () ->
                new GetClassStudentProfileQueryHandler(classes, assembler)
                    .get(instructor, classA, an.studentId()))
        .satisfies(e -> assertCode(e, "IDT-507"));
  }

  @Test
  void anAssignmentFailureIs503() {
    when(lookup.assignmentsOf(any())).thenThrow(new IllegalStateException("x"));

    assertThatThrownBy(() -> new ListClassesSummaryQueryHandler(assembler).summary(instructor))
        .satisfies(e -> assertCode(e, "IDT-507"));
  }

  /** The four reads that span every class of the instructor, #56 to #59. */
  private List<ThrowingCallable> scopeWideReads() {
    return List.of(
        () -> new ListClassesSummaryQueryHandler(assembler).summary(instructor),
        () -> new GetClassScoreTrendQueryHandler(assembler).trend(instructor, null),
        () -> new ListClassAttentionStudentsQueryHandler(assembler).list(instructor, null),
        () ->
            new ListInstructorStudentsQueryHandler(classes, assembler)
                .list(new InstructorStudentsQuery(instructor, null, null, null, null)));
  }

  @Test
  void everyScopeWideReadIs503WhenTheJudgeFails() {
    doThrow(new IllegalStateException("x")).when(metrics).read(any(), any(), any(), anyInt());

    for (ThrowingCallable read : scopeWideReads()) {
      assertThatThrownBy(read).satisfies(e -> assertCode(e, "IDT-507"));
    }
  }

  @Test
  void everyScopeWideReadIs503WhenTheAssignmentsFail() {
    when(lookup.assignmentsOf(any())).thenThrow(new IllegalStateException("x"));

    for (ThrowingCallable read : scopeWideReads()) {
      assertThatThrownBy(read).satisfies(e -> assertCode(e, "IDT-507"));
    }
  }

  @Test
  void progressCountsEveryLabelAndMeansCompletion() {
    var progress = new GetClassProgressQueryHandler(assembler).get(instructor, classA);

    assertThat(progress.studentCount()).isEqualTo(2);
    assertThat(progress.completionPercent()).isEqualTo(50);
    assertThat(progress.avgScore()).isEqualTo(7.5);
    assertThat(progress.statusCounts().get(ClassStudentStatus.ABSENT)).isEqualTo(1);
    assertThat(progress.statusCounts().get(ClassStudentStatus.ON_TRACK)).isEqualTo(1);
    assertThat(progress.statusCounts().get(ClassStudentStatus.WATCH)).isZero();
  }

  @Test
  void summaryCountsPeopleOnceAndAbsentOnce() {
    var summary = new ListClassesSummaryQueryHandler(assembler).summary(instructor);

    assertThat(summary.classCount()).isEqualTo(2);
    assertThat(summary.studentCount()).isEqualTo(2);
    assertThat(summary.absentStudentCount()).isEqualTo(1);
    assertThat(summary.items()).extracting(c -> c.owned().id()).containsExactly(classA, classB);
    assertThat(summary.items().get(0).absentCount()).isEqualTo(1);
    assertThat(summary.items().get(1).absentCount()).isZero();
    assertThat(summary.items().get(1).completionPercent()).isEqualTo(100);
  }

  @Test
  void summaryOfAnInstructorWithoutClassesIsEmptyAndNullAverage() {
    var none = UUID.randomUUID();
    when(classes.listOwned(none)).thenReturn(List.of());
    when(lookup.assignmentsOf(any())).thenReturn(List.of());

    var summary = new ListClassesSummaryQueryHandler(assembler).summary(none);

    assertThat(summary.classCount()).isZero();
    assertThat(summary.avgCompletionPercent()).isNull();
    assertThat(summary.items()).isEmpty();
  }

  @Test
  void anEmptyClassHasNoFiguresAndAskTheJudgeNothing() {
    when(classes.listActiveRoster(classB)).thenReturn(List.of());

    var progress = new GetClassProgressQueryHandler(assembler).get(instructor, classB);

    assertThat(progress.studentCount()).isZero();
    assertThat(progress.completionPercent()).isNull();
    assertThat(progress.avgScore()).isNull();
  }

  @Test
  void trendHasSixPointsAndNullClassForAllClasses() {
    var all = new GetClassScoreTrendQueryHandler(assembler).trend(instructor, null);
    var one = new GetClassScoreTrendQueryHandler(assembler).trend(instructor, classA);

    assertThat(all.classId()).isNull();
    assertThat(all.className()).isNull();
    assertThat(all.points()).hasSize(6);
    assertThat(all.points().get(0).avgScore()).isNull();
    assertThat(all.points().get(5).avgScore()).isEqualTo(8.3);
    assertThat(one.className()).isEqualTo("Class A");
    assertThatThrownBy(
            () -> new GetClassScoreTrendQueryHandler(assembler).trend(UUID.randomUUID(), classA))
        .satisfies(e -> assertCode(e, "IDT-414"));
  }

  @Test
  void trendOfAnEmptyScopeUsesTheClockForItsWeeks() {
    var none = UUID.randomUUID();
    when(classes.listOwned(none)).thenReturn(List.of());
    when(lookup.assignmentsOf(any())).thenReturn(List.of());

    var trend = new GetClassScoreTrendQueryHandler(assembler).trend(none, null);

    assertThat(trend.points()).hasSize(6);
    assertThat(trend.points().get(5).weekStart()).isEqualTo(LocalDate.of(2026, 10, 26));
    assertThat(trend.points()).extracting(p -> p.avgScore()).containsOnlyNulls();
  }

  @Test
  void attentionListsTheAbsentStudentOfTheScope() {
    var list = new ListClassAttentionStudentsQueryHandler(assembler).list(instructor, null);

    assertThat(list).hasSize(1);
    assertThat(list.get(0).studentId()).isEqualTo(binh.studentId());
    assertThat(list.get(0).reasonValue()).isEqualTo(10);
  }

  @Test
  void studentsAreSortedByNameIgnoringCaseAndPagedBeforeTheJudgeIsAsked() {
    var handler = new ListInstructorStudentsQueryHandler(classes, assembler);

    var first = handler.list(new InstructorStudentsQuery(instructor, null, null, 1, 2));
    var second = handler.list(new InstructorStudentsQuery(instructor, null, null, 2, 2));

    assertThat(first.totalCount()).isEqualTo(3);
    assertThat(first.totalPages()).isEqualTo(2);
    assertThat(first.items())
        .extracting(r -> r.student().displayName() + "/" + r.className())
        .containsExactlyInAnyOrder("An/Class A", "An/Class B");
    assertThat(second.items()).extracting(r -> r.student().displayName()).containsExactly("binh");
  }

  @Test
  void studentSearchFiltersOnTheNameAndAClassFilterKeepsOneClass() {
    var handler = new ListInstructorStudentsQueryHandler(classes, assembler);

    var named = handler.list(new InstructorStudentsQuery(instructor, null, "  BIN ", null, null));
    var scoped = handler.list(new InstructorStudentsQuery(instructor, classB, null, null, null));
    var none = handler.list(new InstructorStudentsQuery(instructor, null, "zzz", null, null));
    var beyond = handler.list(new InstructorStudentsQuery(instructor, null, null, 9, 5));

    assertThat(named.items()).hasSize(1);
    assertThat(scoped.items()).hasSize(1);
    assertThat(scoped.pageSize()).isEqualTo(20);
    assertThat(none.items()).isEmpty();
    assertThat(none.totalPages()).isZero();
    assertThat(beyond.items()).isEmpty();
    assertThat(beyond.totalCount()).isEqualTo(3);
  }

  @Test
  void studentParametersOutOfRangeAreIdtV17() {
    var handler = new ListInstructorStudentsQueryHandler(classes, assembler);

    for (InstructorStudentsQuery bad :
        List.of(
            new InstructorStudentsQuery(instructor, null, null, 0, null),
            new InstructorStudentsQuery(instructor, null, null, null, 0),
            new InstructorStudentsQuery(instructor, null, null, null, 101),
            new InstructorStudentsQuery(instructor, null, "x".repeat(101), null, null))) {
      assertThatThrownBy(() -> handler.list(bad)).satisfies(e -> assertCode(e, "IDT-V17"));
    }
    assertThatThrownBy(
            () ->
                handler.list(
                    new InstructorStudentsQuery(instructor, UUID.randomUUID(), null, null, null)))
        .satisfies(e -> assertCode(e, "IDT-414"));
  }

  @Test
  void rosterCarriesTheFiguresWhenTheyCanBeRead() {
    var assemblerAlive = assembler;
    var handler = new ListClassRosterQueryHandler(classes, assemblerAlive);

    var rows = handler.list(instructor, classA);

    assertThat(rows)
        .extracting(r -> r.status())
        .containsExactly(ClassStudentStatus.ON_TRACK, ClassStudentStatus.ABSENT);
  }
}
