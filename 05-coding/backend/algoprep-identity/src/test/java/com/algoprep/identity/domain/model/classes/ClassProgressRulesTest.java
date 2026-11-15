package com.algoprep.identity.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import com.algoprep.identity.domain.model.StudentMetrics.AssignmentOutcome;
import com.algoprep.identity.domain.model.StudentMetrics.WeekPoint;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** BR-10 labels, BR-12 completion and the ordering of the attention list at a fixed clock. */
class ClassProgressRulesTest {
  private static final Instant NOW = Instant.parse("2026-11-07T05:00:00Z");
  private static final UUID CLASS = UUID.randomUUID();

  private static List<WeekPoint> weeks(Double... ratios) {
    List<WeekPoint> list = new ArrayList<>();
    for (int i = 0; i < ratios.length; i++) {
      list.add(new WeekPoint(LocalDate.of(2026, 9, 28).plusWeeks(i), ratios[i]));
    }
    return list;
  }

  private static AssignmentOutcome outcome(boolean accepted) {
    return new AssignmentOutcome(
        UUID.randomUUID(), NOW.minusSeconds(86_400L * 30), BigDecimal.ONE, "ACCEPTED", accepted);
  }

  private static StudentMetrics metrics(
      Instant last, Double ratio, List<WeekPoint> weeks, AssignmentOutcome... assigned) {
    return new StudentMetrics(UUID.randomUUID(), last, ratio, 0, weeks, List.of(assigned));
  }

  private static ClassStudentRow row(StudentMetrics m, String name) {
    return ClassProgressRules.row(
        CLASS,
        "A",
        new ClassRosterEntry(m.studentId(), name, "e@x.test", null, NOW.minusSeconds(86_400L * 60)),
        m,
        NOW);
  }

  @Test
  void noSubmissionInScopeIsInsufficientDataEvenWithAssignments() {
    var m = metrics(null, null, weeks(null, null), outcome(false));

    assertThat(row(m, "n").status()).isEqualTo(ClassStudentStatus.INSUFFICIENT_DATA);
    assertThat(row(m, "n").completionPercent()).isZero();
    assertThat(row(m, "n").avgScore()).isNull();
  }

  @Test
  void sevenDaysIsTheSlidingBoundaryOfAbsent() {
    var inside = metrics(NOW.minusSeconds(7 * 86_400L), 1.0, weeks(1.0, 1.0), outcome(true));
    var outside = metrics(NOW.minusSeconds(7 * 86_400L + 1), 1.0, weeks(1.0, 1.0), outcome(true));

    assertThat(row(inside, "a").status()).isEqualTo(ClassStudentStatus.ON_TRACK);
    assertThat(row(outside, "a").status()).isEqualTo(ClassStudentStatus.ABSENT);
  }

  @Test
  void absentBeatsNeedsSupportAndNeedsSupportBeatsWatch() {
    Instant recent = NOW.minusSeconds(3600);
    var falling = weeks(0.9, 0.6);

    assertThat(
            row(metrics(NOW.minusSeconds(9 * 86_400L), 0.5, falling, outcome(false)), "x").status())
        .isEqualTo(ClassStudentStatus.ABSENT);
    assertThat(row(metrics(recent, 0.5, falling, outcome(false)), "x").status())
        .isEqualTo(ClassStudentStatus.NEEDS_SUPPORT);
    assertThat(row(metrics(recent, 0.5, weeks(0.5, 0.6), outcome(false)), "x").status())
        .isEqualTo(ClassStudentStatus.WATCH);
    assertThat(
            row(metrics(recent, 0.5, weeks(0.5, 0.6), outcome(true), outcome(true)), "x").status())
        .isEqualTo(ClassStudentStatus.ON_TRACK);
  }

  @Test
  void watchNeedsAnAssignmentAndCompletionBelowHalf() {
    Instant recent = NOW.minusSeconds(3600);

    assertThat(row(metrics(recent, 1.0, weeks(1.0, 1.0)), "x").status())
        .isEqualTo(ClassStudentStatus.ON_TRACK);
    assertThat(
            row(metrics(recent, 1.0, weeks(1.0, 1.0), outcome(true), outcome(false)), "x").status())
        .isEqualTo(ClassStudentStatus.ON_TRACK);
    assertThat(
            row(
                    metrics(
                        recent,
                        1.0,
                        weeks(1.0, 1.0),
                        outcome(true),
                        outcome(false),
                        outcome(false)),
                    "x")
                .status())
        .isEqualTo(ClassStudentStatus.WATCH);
  }

  @Test
  void declineIsTheTrailingStrictlyFallingRunOfAtLeastTwoWeeks() {
    assertThat(ClassProgressRules.decline(weeks(0.9, 0.7, 0.5)))
        .isEqualTo(new ClassProgressRules.Decline(3, 40.0));
    assertThat(ClassProgressRules.decline(weeks(0.5, 0.9, 0.8)).weeks()).isEqualTo(2);
    assertThat(ClassProgressRules.decline(weeks(0.9, 0.9))).isNull();
    assertThat(ClassProgressRules.decline(weeks(0.5, 0.9))).isNull();
    assertThat(ClassProgressRules.decline(weeks(0.9, null))).isNull();
    assertThat(ClassProgressRules.decline(weeks(null, 0.9))).isNull();
    assertThat(ClassProgressRules.decline(weeks(0.9))).isNull();
  }

  @Test
  void scoreIsTheAcceptedRatioOnTenRoundedToOneDecimal() {
    assertThat(ClassProgressRules.avgScore(metrics(NOW, 2.0 / 3.0, weeks()))).isEqualTo(6.7);
    assertThat(ClassProgressRules.avgScore(metrics(NOW, null, weeks()))).isNull();
  }

  @Test
  void meansSkipStudentsWithoutAFigure() {
    var done = row(metrics(NOW, 1.0, weeks(), outcome(true)), "a");
    var none = row(metrics(NOW, 0.5, weeks(), outcome(false)), "b");
    var bare = row(metrics(null, null, weeks()), "c");

    assertThat(ClassProgressRules.meanCompletionPercent(List.of(done, none, bare))).isEqualTo(50);
    assertThat(ClassProgressRules.meanScore(List.of(done, none, bare))).isEqualTo(7.5);
    assertThat(ClassProgressRules.meanCompletionPercent(List.of(bare))).isNull();
    assertThat(ClassProgressRules.meanScore(List.of(bare))).isNull();
    assertThat(ClassProgressRules.meanScore(List.of())).isNull();
  }

  @Test
  void weeklyScoresAverageOnlyStudentsWithAWeekAndNeverUseZero() {
    var a = row(metrics(NOW, 1.0, weeks(1.0, null, 0.5, null, null, 1.0)), "a");
    var b = row(metrics(NOW, 1.0, weeks(0.5, null, null, null, null, 1.0)), "b");

    List<Double> scores = ClassProgressRules.weeklyScores(List.of(a, b));

    assertThat(scores).containsExactly(7.5, null, 5.0, null, null, 10.0);
    assertThat(ClassProgressRules.weeklyScores(List.of())).hasSize(6).containsOnlyNulls();
    assertThat(
            ClassProgressRules.weeklyScores(
                List.of(ClassStudentRow.plain(CLASS, "A", a.student()))))
        .containsOnlyNulls();
  }

  @Test
  void weekStartsAreSixCompletedMondaysAtUtcPlusSeven() {
    List<LocalDate> starts = ClassProgressRules.weekStarts(NOW);

    assertThat(starts).hasSize(6);
    assertThat(starts.get(5)).isEqualTo(LocalDate.of(2026, 10, 26));
    assertThat(starts.get(0)).isEqualTo(LocalDate.of(2026, 9, 21));
  }

  @Test
  void attentionIsOrderedAbsentThenNeedsSupportThenWatchAndCutAtFive() {
    Instant recent = NOW.minusSeconds(3600);
    var absent3 =
        row(metrics(NOW.minusSeconds(3 * 86_400L + 8 * 86_400L), 1.0, weeks(1.0, 1.0)), "a3");
    var absent9 = row(metrics(NOW.minusSeconds(17 * 86_400L), 1.0, weeks(1.0, 1.0)), "a9");
    var supportSmall = row(metrics(recent, 0.5, weeks(0.9, 0.8), outcome(true)), "s1");
    var supportBig = row(metrics(recent, 0.5, weeks(0.9, 0.5, 0.2), outcome(true)), "s2");
    var watchLow = row(metrics(recent, 0.5, weeks(0.5, 0.6), outcome(false), outcome(false)), "w0");
    var watchHigh =
        row(
            metrics(recent, 0.5, weeks(0.5, 0.6), outcome(true), outcome(false), outcome(false)),
            "w1");
    var fine = row(metrics(recent, 1.0, weeks(1.0, 1.0), outcome(true)), "ok");

    var list =
        ClassProgressRules.attention(
            List.of(fine, watchHigh, watchLow, supportSmall, absent3, supportBig, absent9), NOW);

    assertThat(list).hasSize(5);
    assertThat(list).extracting(AttentionView::name).containsExactly("a9", "a3", "s2", "s1", "w0");
    assertThat(list.get(0).reasonCode()).isEqualTo(ClassProgressRules.REASON_ABSENT_DAYS);
    assertThat(list.get(0).reasonValue()).isEqualTo(17);
    assertThat(list.get(2).reasonCode()).isEqualTo(ClassProgressRules.REASON_SCORE_DROP_WEEKS);
    assertThat(list.get(2).reasonValue()).isEqualTo(3);
    assertThat(list.get(4).reasonCode()).isEqualTo(ClassProgressRules.REASON_LOW_COMPLETION);
    assertThat(list.get(4).reasonValue()).isZero();
  }

  @Test
  void equalSeverityFallsBackToTheStudentIdForAStableOrder() {
    var one = row(metrics(NOW.minusSeconds(20 * 86_400L), 1.0, weeks()), "one");
    var two = row(metrics(NOW.minusSeconds(20 * 86_400L), 1.0, weeks()), "two");

    var list = ClassProgressRules.attention(List.of(two, one), NOW);

    assertThat(list.get(0).studentId().compareTo(list.get(1).studentId())).isNegative();
  }

  /** Reads the display name off an attention line. */
  private interface AttentionView {
    static String name(ClassMetricViews.AttentionStudent s) {
      return s.displayName();
    }
  }
}
