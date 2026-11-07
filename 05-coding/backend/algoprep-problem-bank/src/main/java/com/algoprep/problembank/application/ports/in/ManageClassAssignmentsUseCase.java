package com.algoprep.problembank.application.ports.in;

import com.algoprep.problembank.application.dto.ClassAssignmentViews;
import java.util.List;
import java.util.UUID;

/** Assigning and removing problems of a class (DD F2 sections 53 and 54). */
public interface ManageClassAssignmentsUseCase {
  /** 1 to 50 ids; each problem is its own transaction, so one failure never blocks the rest. */
  ClassAssignmentViews.AssignResult assign(UUID instructorId, UUID classId, List<UUID> problemIds);

  /** Soft removal; 409 {@code PRB-025} when the problem is not actively assigned. */
  void remove(UUID instructorId, UUID classId, UUID problemId);
}
