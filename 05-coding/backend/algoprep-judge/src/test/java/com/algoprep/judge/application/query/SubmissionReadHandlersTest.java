package com.algoprep.judge.application.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.algoprep.common.exception.AlgoPrepException;
import com.algoprep.judge.application.dto.ClassAssignment;
import com.algoprep.judge.application.dto.SubmissionReads.Filters;
import com.algoprep.judge.application.dto.SubmissionReads.Membership;
import com.algoprep.judge.application.dto.SubmissionReads.Problem;
import com.algoprep.judge.application.ports.out.ClassAssignmentReadPort;
import com.algoprep.judge.application.ports.out.ClassScopeReadPort;
import com.algoprep.judge.application.ports.out.ProblemSummaryReadPort;
import com.algoprep.judge.domain.model.ScopedSubmission;
import com.algoprep.judge.domain.model.SubmissionReadState;
import com.algoprep.judge.domain.ports.out.InstructorSubmissionRepository;
import com.algoprep.judge.domain.ports.out.SubmissionReadRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class SubmissionReadHandlersTest {
  private final UUID owner = UUID.randomUUID();
  private final UUID id = UUID.randomUUID();
  private final UUID problem = UUID.randomUUID();
  private final UUID testcase = UUID.randomUUID();
  private final UUID classId = UUID.randomUUID();
  private final Instant now = Instant.parse("2026-11-04T00:00:00Z");
  private final SubmissionReadRepository repository = mock(SubmissionReadRepository.class);
  private final ProblemSummaryReadPort problems = mock(ProblemSummaryReadPort.class);
  private final SubmissionReadProjection projection =
      new SubmissionReadProjection(repository, problems);
  private final ReadOwnedSubmissionQueryHandler owned =
      new ReadOwnedSubmissionQueryHandler(repository, projection);

  private SubmissionReadState state(String status, String model, boolean expired) {
    return new SubmissionReadState(
        id,
        owner,
        problem,
        "JAVA",
        model,
        status,
        now,
        now,
        "private source",
        "safe compile error",
        1,
        List.of("PENDING", "COMPILING", "COMPILE_ERROR").contains(status) ? null : 2,
        new BigDecimal("0.500"),
        100,
        256,
        expired);
  }

  private SubmissionReadState.Case testcase(boolean sample, String verdict) {
    return new SubmissionReadState.Case(
        testcase, 1, verdict, 100, 256, sample, "sample output", "sample stderr");
  }

  @Test
  void missingOwnerAndMissingProgressUseIdenticalErrors() {
    when(repository.owned(id, owner)).thenReturn(Optional.empty());
    assertThatThrownBy(() -> owned.get(owner, id)).satisfies(e -> code(e, "JDG-110"));
    assertThatThrownBy(() -> owned.getProgress(owner, id)).satisfies(e -> code(e, "JDG-110"));
    verify(repository, org.mockito.Mockito.times(2)).owned(id, owner);
  }

  @Test
  void acceptedBeatsRoundHalfUpAndSummariesRemainOptional() {
    when(repository.owned(id, owner))
        .thenReturn(Optional.of(state("ACCEPTED", "STANDARD_IO", false)));
    when(repository.beats(any())).thenReturn(new SubmissionReadState.Beats(40, 19));
    when(repository.cases(id)).thenReturn(List.of(testcase(true, "AC")));
    when(problems.currentSamples(any(), any())).thenReturn(Set.of(testcase));
    when(problems.summaries(any())).thenThrow(new IllegalStateException("private SQL"));
    var result = owned.get(owner, id);
    assertThat(result.beatsPercent()).isEqualTo(48);
    assertThat(result.beatsSampleSize()).isEqualTo(40);
    assertThat(result.problem()).isNull();
    assertThat(result.testcaseDetail().items().getFirst().stderrSnippet()).isNull();
    assertThat(result.toString()).doesNotContain("private source");
    when(repository.beats(any())).thenReturn(new SubmissionReadState.Beats(9, 8));
    assertThat(owned.get(owner, id).beatsPercent()).isNull();
  }

  @ParameterizedTest
  @ValueSource(strings = {"STANDARD_IO", "FUNCTION_WRAPPER"})
  void sampleOutputRequiresBothVisibilityGuardsAndStderrRequiresModelAndFlag(String model) {
    when(repository.cases(id)).thenReturn(List.of(testcase(true, "WA")));
    when(problems.currentSamples(any(), any())).thenReturn(Set.of(testcase));
    when(repository.stderrEnabled()).thenReturn(true);
    var s = state("WRONG_ANSWER", model, false);
    var line = projection.detail(s, false).testcaseDetail().items().getFirst();
    assertThat(line.actualOutput()).isEqualTo("sample output");
    assertThat(line.stderrSnippet())
        .isEqualTo(model.equals("STANDARD_IO") ? "sample stderr" : null);
    when(repository.stderrEnabled()).thenReturn(false);
    assertThat(projection.detail(s, false).testcaseDetail().items().getFirst().stderrSnippet())
        .isNull();
    when(problems.currentSamples(any(), any())).thenReturn(Set.of());
    assertHidden(projection.detail(s, false).testcaseDetail().items().getFirst());
    when(problems.currentSamples(any(), any())).thenReturn(Set.of(testcase));
    when(repository.cases(id)).thenReturn(List.of(testcase(false, "WA")));
    assertHidden(projection.detail(s, false).testcaseDetail().items().getFirst());
    when(problems.currentSamples(any(), any())).thenThrow(new IllegalStateException("private"));
    assertHidden(projection.detail(s, false).testcaseDetail().items().getFirst());
  }

  private static void assertHidden(
      com.algoprep.judge.application.dto.SubmissionReads.Testcase line) {
    assertThat(line.visibility()).isEqualTo("HIDDEN");
    assertThat(line.testcaseId()).isNull();
    assertThat(line.actualOutput()).isNull();
    assertThat(line.stderrSnippet()).isNull();
  }

  @Test
  void retentionAndNotApplicableDoNotRemoveSourceOrScores() {
    var expired = projection.detail(state("WRONG_ANSWER", "STANDARD_IO", true), false);
    assertThat(expired.testcaseDetail().unavailableReason()).isEqualTo("RETENTION_EXPIRED");
    assertThat(expired.sourceCode()).isEqualTo("private source");
    assertThat(expired.scoreRatio()).isEqualByComparingTo("0.5");
    when(repository.cases(id)).thenReturn(List.of());
    for (String status : List.of("COMPILE_ERROR", "SYSTEM_ERROR")) {
      var result = projection.detail(state(status, "STANDARD_IO", false), false);
      assertThat(result.testcaseDetail().unavailableReason()).isEqualTo("NOT_APPLICABLE");
      assertThat(result.scoreRatio()).isNull();
    }
    var pending = projection.detail(state("PENDING", "STANDARD_IO", false), false);
    assertThat(pending.testcaseDetail().available()).isTrue();
    assertThat(pending.testcaseDetail().items()).isEmpty();
  }

  @Test
  void progressUsesOnlyOrderVerdictAndMatchesFinalSequenceWithPartialErrors() {
    when(repository.cases(id))
        .thenReturn(
            List.of(
                testcase(true, "WA"),
                new SubmissionReadState.Case(
                    UUID.randomUUID(), 2, "AC", 100, 256, false, null, null)));
    when(repository.owned(id, owner))
        .thenReturn(Optional.of(state("JUDGING", "STANDARD_IO", false)));
    var progress = owned.getProgress(owner, id);
    assertThat(progress.seq()).isEqualTo(4);
    assertThat(progress.completedCount()).isEqualTo(2);
    assertThat(progress.passedCount()).isEqualTo(1);
    assertThat(progress.scoreRatio()).isNull();
    var finished = projection.progress(state("WRONG_ANSWER", "STANDARD_IO", false));
    assertThat(finished.seq()).isEqualTo(5);
    assertThat(finished.firstFailingOrder()).isEqualTo(1);
    var system = projection.progress(state("SYSTEM_ERROR", "STANDARD_IO", false));
    assertThat(system.seq()).isEqualTo(5);
    assertThat(system.scoreRatio()).isNull();
    assertThat(system.firstFailingOrder()).isNull();
    assertThat(projection.progress(state("PENDING", "STANDARD_IO", false)).seq()).isZero();
    assertThat(projection.progress(state("COMPILING", "STANDARD_IO", false)).seq()).isEqualTo(1);
    assertThat(projection.progress(state("WRONG_ANSWER", "STANDARD_IO", true)).testcases())
        .isEmpty();
  }

  @Test
  void historiesClampPageNormalizeFiltersAndUseBatchSummaries() {
    var handler = new ReadMySubmissionHistoryQueryHandler(repository, projection, problems);
    var state = state("WRONG_ANSWER", "STANDARD_IO", false);
    when(repository.count(owner, null, null, null)).thenReturn(45L);
    when(repository.history(eq(owner), any(), any(), any())).thenReturn(List.of(state));
    when(problems.summaries(any()))
        .thenReturn(List.of(new Problem(problem, "sum", "Sum", "sum", "Easy", true)));
    var result = handler.history(owner, new Filters(" ", "UNKNOWN", "NO", "99", null));
    assertThat(result.page()).isEqualTo(3);
    assertThat(result.items().getFirst().problem().withdrawn()).isTrue();
    when(problems.search("sum", 500)).thenReturn(List.of(problem));
    when(repository.history(eq(owner), eq("ACCEPTED"), eq("JAVA"), any())).thenReturn(List.of());
    handler.history(owner, new Filters(" sum ", "ACCEPTED", "JAVA", "bad", null));
    verify(repository).count(owner, "ACCEPTED", "JAVA", Set.of(problem));
    assertThatThrownBy(() -> handler.recent(owner, problem, 11)).satisfies(e -> code(e, "JDG-V14"));
    assertThatThrownBy(() -> handler.recent(owner, problem, 0)).satisfies(e -> code(e, "JDG-V14"));
    when(repository.recent(owner, problem, 10)).thenReturn(List.of(state));
    assertThat(handler.recent(owner, problem, 10)).hasSize(1);
  }

  @Test
  void pendingAndCompileSummaryFieldsStayNullAndHelpersNormalizeBoundaries() {
    assertThat(
            SubmissionReadProjection.summary(state("PENDING", "STANDARD_IO", false)).passedCount())
        .isNull();
    assertThat(
            SubmissionReadProjection.summary(state("COMPILE_ERROR", "STANDARD_IO", false))
                .runtimeMs())
        .isNull();
    assertThat(SubmissionReadQueries.page("-3", 0)).isEqualTo(1);
    assertThat(SubmissionReadQueries.page(null, 0)).isEqualTo(1);
    assertThat(SubmissionReadQueries.keyword("x".repeat(101))).hasSize(100);
    assertThat(SubmissionReadQueries.id(id.toString(), "JDG-110")).isEqualTo(id);
    assertThatThrownBy(() -> SubmissionReadQueries.id("bad", "JDG-110"))
        .satisfies(e -> code(e, "JDG-110"));
  }

  @Test
  void instructorScopeAndDetailsNeverTrustBreadcrumbsOrInventMemberships() {
    var scopePort = mock(ClassScopeReadPort.class);
    var assignments = mock(ClassAssignmentReadPort.class);
    var scoped = mock(InstructorSubmissionRepository.class);
    var scope = new InstructorSubmissionScope(scopePort, assignments);
    var member =
        new Membership(
            classId, "Class", owner, "Student", now.minusSeconds(20), now.plusSeconds(20));
    when(scopePort.read(owner)).thenReturn(List.of(member));
    when(assignments.listAssignments(any()))
        .thenReturn(
            List.of(new ClassAssignment(classId, problem, "Sum", now.minusSeconds(10), null)));
    var row =
        new ScopedSubmission(
            id, classId, owner, problem, "JAVA", "WRONG_ANSWER", 1, 2, new BigDecimal("0.5"), now);
    when(scoped.list(any(), anyInt(), anyInt())).thenReturn(List.of(row));
    when(scoped.count(any())).thenReturn(1L);
    when(repository.find(id)).thenReturn(Optional.of(state("WRONG_ANSWER", "STANDARD_IO", false)));
    var handler = new ReadInstructorSubmissionsQueryHandler(scope, scoped, repository, projection);
    assertThat(
            handler
                .list(owner, new Filters("sum", "failed", null, "1", classId))
                .items()
                .getFirst()
                .leftClass())
        .isTrue();
    assertThat(handler.detail(owner, id).member()).isEqualTo(member);
    assertThat(handler.classHistory(owner, classId, owner, "1").items().getFirst().problemTitle())
        .isEqualTo("Sum");
    assertThatThrownBy(() -> handler.classHistory(owner, UUID.randomUUID(), owner, "1"))
        .satisfies(e -> code(e, "JDG-111"));
    assertThatThrownBy(() -> handler.classHistory(owner, classId, UUID.randomUUID(), "1"))
        .satisfies(e -> code(e, "JDG-112"));
    when(scoped.list(any(), anyInt(), anyInt())).thenReturn(List.of());
    assertThatThrownBy(() -> handler.detail(owner, id)).satisfies(e -> code(e, "JDG-110"));
    when(scopePort.read(owner)).thenThrow(new IllegalStateException("private scope"));
    assertThatThrownBy(() -> handler.detail(owner, id))
        .isInstanceOf(AlgoPrepException.class)
        .hasMessage("Submission scope unavailable")
        .hasNoCause();
  }

  private static void code(Throwable error, String expected) {
    assertThat(error)
        .isInstanceOfSatisfying(
            AlgoPrepException.class, e -> assertThat(e.errorCode()).isEqualTo(expected));
  }
}
