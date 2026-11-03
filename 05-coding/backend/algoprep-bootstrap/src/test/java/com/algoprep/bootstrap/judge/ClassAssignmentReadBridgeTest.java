package com.algoprep.bootstrap.judge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.algoprep.problembank.domain.model.ClassAssignmentSnapshot;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;

class ClassAssignmentReadBridgeTest {
  private final UUID classId = UUID.randomUUID();
  private final UUID problemId = UUID.randomUUID();

  @Test
  void mapsEveryFieldAndKeepsBothRemovedAndCurrentIntervals() {
    Instant assigned = Instant.parse("2026-10-01T00:00:00.123456Z");
    Instant removed = assigned.plusSeconds(3600);
    var bridge =
        new ClassAssignmentReadBridge(
            ids -> {
              assertThat(ids).containsExactly(classId);
              return List.of(
                  new ClassAssignmentSnapshot(classId, problemId, "Đề bài", assigned, removed),
                  new ClassAssignmentSnapshot(classId, problemId, "Đề bài", removed, null));
            });
    var result = bridge.listAssignments(List.of(classId, classId));
    assertThat(result).hasSize(2);
    assertThat(result.getFirst().classId()).isEqualTo(classId);
    assertThat(result.getFirst().problemId()).isEqualTo(problemId);
    assertThat(result.getFirst().problemTitle()).isEqualTo("Đề bài");
    assertThat(result.getFirst().assignedAt()).isEqualTo(assigned);
    assertThat(result.getFirst().removedAt()).isEqualTo(removed);
    assertThat(result.getLast().removedAt()).isNull();
    assertThatThrownBy(result::clear).isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  void emptyInputAvoidsCallingF2() {
    var called = new AtomicBoolean();
    var bridge =
        new ClassAssignmentReadBridge(
            ids -> {
              called.set(true);
              return List.of();
            });
    assertThat(bridge.listAssignments(List.of())).isEmpty();
    assertThat(called).isFalse();
  }

  @Test
  void invalidInputCannotTriggerAnUnscopedLookup() {
    var called = new AtomicBoolean();
    var bridge =
        new ClassAssignmentReadBridge(
            ids -> {
              called.set(true);
              return List.of();
            });
    assertThatThrownBy(() -> bridge.listAssignments(null)).isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> bridge.listAssignments(Arrays.asList(classId, null)))
        .isInstanceOf(NullPointerException.class);
    assertThat(called).isFalse();
  }

  @Test
  void upstreamFailuresAreSanitizedAndStillFailClosed() {
    var bridge =
        new ClassAssignmentReadBridge(
            ids -> {
              throw new IllegalStateException("SELECT private_source FROM secret_database");
            });
    assertThatThrownBy(() -> bridge.listAssignments(List.of(classId)))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Class assignments are unavailable")
        .hasNoCause();
  }

  @Test
  void missingClassCanLegitimatelyReturnNoAssignments() {
    var bridge = new ClassAssignmentReadBridge(ids -> List.of());
    assertThat(bridge.listAssignments(List.of(classId))).isEmpty();
  }
}
