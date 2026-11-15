package com.algoprep.judge.domain.model;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** Trusted class scope supplied by identity after checking CLASS_MANAGEMENT and ownership. */
public record ClassMetricsScope(
    UUID classId, List<Student> students, List<Assignment> assignments, int weekCount) {
  public ClassMetricsScope {
    Objects.requireNonNull(classId);
    students = List.copyOf(students);
    assignments = List.copyOf(assignments);
    if (weekCount < 1 || weekCount > 12) {
      throw new IllegalArgumentException("Invalid class metrics week count");
    }
    var ids = new HashSet<UUID>();
    for (var student : students) {
      if (!ids.add(student.studentId())) {
        throw new IllegalArgumentException("Duplicate metrics student");
      }
    }
    assignments = assignments.stream().distinct().toList();
  }

  public record Student(UUID studentId, Instant joinedAt, Instant leftAt) {
    public Student {
      Objects.requireNonNull(studentId);
      Objects.requireNonNull(joinedAt);
      if (leftAt != null && leftAt.isBefore(joinedAt)) {
        throw new IllegalArgumentException("Invalid enrollment window");
      }
    }
  }

  public record Assignment(UUID problemId, Instant assignedAt, Instant removedAt) {
    public Assignment {
      Objects.requireNonNull(problemId);
      Objects.requireNonNull(assignedAt);
      if (removedAt != null && removedAt.isBefore(assignedAt)) {
        throw new IllegalArgumentException("Invalid assignment window");
      }
    }
  }
}
