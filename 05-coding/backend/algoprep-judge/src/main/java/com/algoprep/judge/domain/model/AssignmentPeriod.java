package com.algoprep.judge.domain.model;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/** A caller-supplied assignment interval, including historical removed assignments. */
public record AssignmentPeriod(
    UUID classId, UUID problemId, Instant assignedAt, Instant removedAt) {
  public AssignmentPeriod {
    Objects.requireNonNull(classId);
    Objects.requireNonNull(problemId);
    Objects.requireNonNull(assignedAt);
    if (removedAt != null && removedAt.isBefore(assignedAt)) {
      throw new IllegalArgumentException("Invalid assignment interval");
    }
  }

  public Key key() {
    return new Key(classId, problemId, assignedAt);
  }

  public static List<AssignmentPeriod> distinct(List<AssignmentPeriod> periods) {
    Map<Key, AssignmentPeriod> unique = new LinkedHashMap<>();
    for (var period : periods) {
      var previous = unique.putIfAbsent(period.key(), period);
      if (previous != null && !previous.equals(period)) {
        throw new IllegalArgumentException("Conflicting assignment interval");
      }
    }
    return List.copyOf(unique.values());
  }

  public record Key(UUID classId, UUID problemId, Instant assignedAt) {}
}
