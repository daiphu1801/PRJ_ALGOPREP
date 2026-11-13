package com.algoprep.judge.domain.metrics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.algoprep.judge.domain.model.ClassMetricSubmission;
import com.algoprep.judge.domain.model.ClassMetricsScope;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ClassSubmissionMetricsCalculatorTest {
  private final UUID student = UUID.randomUUID();
  private final UUID problem = UUID.randomUUID();
  private final Instant now = Instant.parse("2026-11-04T05:00:00Z");

  private ClassMetricsScope scope(int weeks) {
    return new ClassMetricsScope(
        UUID.randomUUID(),
        List.of(new ClassMetricsScope.Student(student, now.minusSeconds(86400 * 100L), null)),
        List.of(new ClassMetricsScope.Assignment(problem, now.minusSeconds(86400 * 100L), null)),
        weeks);
  }

  private ClassMetricSubmission row(String at, String verdict, String score) {
    return new ClassMetricSubmission(
        UUID.randomUUID(),
        student,
        problem,
        verdict,
        score == null ? null : new BigDecimal(score),
        Instant.parse(at));
  }

  @Test
  void completeWeeksUseLocalMondayAndKeepZeroBucketsWithoutTheCurrentWeek() {
    var rows =
        List.of(
            row("2026-10-25T17:00:00Z", "ACCEPTED", "1"),
            row("2026-11-01T16:59:59Z", "COMPILE_ERROR", null),
            row("2026-11-01T17:00:00Z", "ACCEPTED", "1"),
            row("2026-10-28T03:00:00Z", "SYSTEM_ERROR", null),
            row("2026-10-28T04:00:00Z", "JUDGING", null));
    var result = ClassSubmissionMetricsCalculator.calculate(scope(2), rows, now).getFirst();
    assertThat(result.weeklyTrend()).hasSize(2);
    assertThat(result.weeklyTrend().getFirst().weekStart())
        .isEqualTo(LocalDate.parse("2026-10-19"));
    assertThat(result.weeklyTrend().getFirst().submissionCount()).isZero();
    assertThat(result.weeklyTrend().getFirst().acceptedRatio()).isNull();
    var last = result.weeklyTrend().getLast();
    assertThat(last.submissionCount()).isEqualTo(4);
    assertThat(last.acceptedCount()).isEqualTo(1);
    assertThat(last.acceptedRatio()).isEqualTo(.5);
    assertThat(result.acceptedRatio()).isEqualTo(2.0 / 3);
  }

  @Test
  void streakRequiresTodayButIncludesPendingAndSystemErrors() {
    var rows =
        List.of(
            row("2026-11-03T17:00:00Z", "PENDING", null),
            row("2026-11-02T17:00:00Z", "SYSTEM_ERROR", null),
            row("2026-11-01T17:00:00Z", "COMPILE_ERROR", null));
    var result = ClassSubmissionMetricsCalculator.calculate(scope(1), rows, now).getFirst();
    assertThat(result.streakDays()).isEqualTo(3);
    assertThat(result.acceptedRatio()).isEqualTo(0.0);
    assertThat(result.lastSubmittedAt()).isEqualTo(Instant.parse("2026-11-03T17:00:00Z"));
    assertThat(
            ClassSubmissionMetricsCalculator.calculate(scope(1), rows.subList(1, 3), now)
                .getFirst()
                .streakDays())
        .isZero();
  }

  @Test
  void assignmentBestIsLocalToEachActiveAssignmentAndBreaksUuidTiesLikePostgres() {
    Instant assigned = Instant.parse("2026-11-03T00:00:00Z");
    var query =
        new ClassMetricsScope(
            UUID.randomUUID(),
            scope(1).students(),
            List.of(new ClassMetricsScope.Assignment(problem, assigned, null)),
            1);
    var earlier = row("2026-11-02T00:00:00Z", "ACCEPTED", "1");
    var lower =
        new ClassMetricSubmission(
            UUID.fromString("7fffffff-0000-4000-8000-000000000001"),
            student,
            problem,
            "WRONG_ANSWER",
            new BigDecimal(".5"),
            now);
    var higher =
        new ClassMetricSubmission(
            UUID.fromString("80000000-0000-4000-8000-000000000001"),
            student,
            problem,
            "RUNTIME_ERROR",
            new BigDecimal(".5"),
            now);
    var best =
        ClassSubmissionMetricsCalculator.calculate(query, List.of(earlier, lower, higher), now)
            .getFirst()
            .assignmentResults()
            .getFirst();
    assertThat(best.bestVerdict()).isEqualTo("RUNTIME_ERROR");
    assertThat(best.bestRatio()).isEqualByComparingTo(".5");
    assertThat(best.accepted()).isFalse();
  }

  @Test
  void emptyAssignmentsAndUnscoredAttemptsHaveNullBestAndNoAcceptedRatio() {
    var result =
        ClassSubmissionMetricsCalculator.calculate(
                scope(12), List.of(row("2026-11-04T00:00:00Z", "SYSTEM_ERROR", null)), now)
            .getFirst();
    assertThat(result.acceptedRatio()).isNull();
    assertThat(result.assignmentResults().getFirst().bestRatio()).isNull();
    assertThat(result.assignmentResults().getFirst().bestVerdict()).isNull();
    assertThat(result.assignmentResults().getFirst().accepted()).isFalse();
    var query = new ClassMetricsScope(UUID.randomUUID(), scope(1).students(), List.of(), 1);
    assertThat(
            ClassSubmissionMetricsCalculator.calculate(query, List.of(), now)
                .getFirst()
                .assignmentResults())
        .isEmpty();
  }

  @Test
  void scopesRejectInvalidBoundsAndDuplicateStudentsAndCopyTheirInputs() {
    assertThatThrownBy(() -> scope(0)).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> scope(13)).isInstanceOf(IllegalArgumentException.class);
    assertThat(scope(12).weekCount()).isEqualTo(12);
    var member = scope(1).students().getFirst();
    assertThatThrownBy(
            () -> new ClassMetricsScope(UUID.randomUUID(), List.of(member, member), List.of(), 1))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new ClassMetricsScope.Student(student, now, now.minusSeconds(1)))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new ClassMetricsScope.Assignment(problem, now, now.minusSeconds(1)))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> scope(1).students().clear())
        .isInstanceOf(UnsupportedOperationException.class);
  }
}
