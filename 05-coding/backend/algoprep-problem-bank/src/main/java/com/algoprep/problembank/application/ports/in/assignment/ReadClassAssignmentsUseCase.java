package com.algoprep.problembank.application.ports.in;

import com.algoprep.problembank.application.dto.ClassAssignmentViews;
import java.util.Map;
import java.util.UUID;

/**
 * Instructor reads of DD F2 sections 50, 51, 52 and 55. The instructor is the token subject; a
 * class that is not theirs is 403 {@code PRB-005}.
 */
public interface ReadClassAssignmentsUseCase {
  /** {@code classId}, {@code q} and {@code page} come from {@code params}. */
  ClassAssignmentViews.AssignmentList list(UUID instructorId, Map<String, String> params);

  ClassAssignmentViews.Summary summary(UUID instructorId);

  ClassAssignmentViews.AssignableList assignable(
      UUID instructorId, UUID classId, Map<String, String> params);

  ClassAssignmentViews.TopList top(UUID instructorId);
}
