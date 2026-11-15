package com.algoprep.judge.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.algoprep.judge.application.dto.AssignedProblemMetricsQuery;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AssignmentPeriodTest {
  private final UUID classId = UUID.randomUUID();
  private final UUID problem = UUID.randomUUID();
  private final Instant at = Instant.parse("2026-10-01T00:00:00Z");

  @Test
  void periodsKeepDistinctAssignmentKeysAndRejectConflictingEndsOrInvalidBounds() {
    var first = new AssignmentPeriod(classId, problem, at, null);
    var again = new AssignmentPeriod(classId, problem, at.plusSeconds(10), null);
    assertThat(AssignmentPeriod.distinct(List.of(first, first, again)))
        .containsExactly(first, again);
    assertThat(first.key()).isNotEqualTo(again.key());
    assertThatThrownBy(
            () ->
                AssignmentPeriod.distinct(
                    List.of(first, new AssignmentPeriod(classId, problem, at, at.plusSeconds(10)))))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new AssignmentPeriod(classId, problem, at, at.minusSeconds(1)))
        .isInstanceOf(IllegalArgumentException.class);
    assertThat(new AssignmentPeriod(classId, problem, at, at).removedAt()).isEqualTo(at);
    assertThatThrownBy(() -> new AssignmentPeriod(null, problem, at, null))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  void queryAndScopeCopyInputsAndDoNotLeakMutableCollections() {
    var periods = new ArrayList<>(List.of(new AssignmentPeriod(classId, problem, at, null)));
    var classes = new HashSet<>(Set.of(classId));
    var problems = new HashSet<>(Set.of(problem));
    var query = new AssignedProblemMetricsQuery(UUID.randomUUID(), classes, problems, periods);
    var scope = new AssignmentMetricScope(List.of(), classes, problems);
    periods.clear();
    classes.clear();
    problems.clear();
    assertThat(query.assignments()).hasSize(1);
    assertThat(query.classIds()).containsExactly(classId);
    assertThat(query.problemIds()).containsExactly(problem);
    assertThat(scope.classIds()).containsExactly(classId);
    assertThat(scope.problemIds()).containsExactly(problem);
    assertThatThrownBy(() -> scope.windows().add(null))
        .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  void zeroDenominatorIsNullAndPercentageHasNoArtificialRounding() {
    assertThat(ClassProblemMetric.from(4, 0, 0)).isEqualTo(new ClassProblemMetric(4, null));
    assertThat(ClassProblemMetric.from(5, 3, 1).acRatePercent()).isEqualTo(100.0 / 3);
    assertThat(ClassProblemMetric.from(5, 3, 0).acRatePercent()).isZero();
  }
}
