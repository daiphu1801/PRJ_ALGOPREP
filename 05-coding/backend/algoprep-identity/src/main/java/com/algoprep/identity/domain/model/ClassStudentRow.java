package com.algoprep.identity.domain.model;

import java.util.UUID;

/**
 * One student of one class as the instructor screens show them. {@code metrics}, {@code status},
 * {@code completion} and {@code avgScore} are null only when the judge could not be asked and the
 * endpoint allows that (the roster of #26); {@code completion} is a fraction of 0 to 1 and is null
 * for a class with no assigned problem; {@code avgScore} is on the 10 point scale.
 */
public record ClassStudentRow(
    UUID classId,
    String className,
    ClassRosterEntry student,
    StudentMetrics metrics,
    ClassStudentStatus status,
    Double completion,
    Double avgScore) {

  /** A row for a roster read where the figures are not available. */
  public static ClassStudentRow plain(UUID classId, String className, ClassRosterEntry student) {
    return new ClassStudentRow(classId, className, student, null, null, null, null);
  }

  /** Whole percent for tables; 0 when no problem is assigned (DD api F1 section 0.8.3). */
  public int completionPercent() {
    return completion == null ? 0 : (int) Math.round(completion * 100);
  }
}
