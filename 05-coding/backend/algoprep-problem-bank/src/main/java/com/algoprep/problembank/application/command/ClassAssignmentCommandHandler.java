package com.algoprep.problembank.application.command;

import com.algoprep.common.exception.AlgoPrepException;
import com.algoprep.common.exception.InvalidRequestException;
import com.algoprep.common.exception.PermissionDeniedException;
import com.algoprep.problembank.application.dto.ClassAssignmentViews.AssignResult;
import com.algoprep.problembank.application.dto.ClassAssignmentViews.Failed;
import com.algoprep.problembank.application.ports.in.ManageClassAssignmentsUseCase;
import com.algoprep.problembank.application.ports.out.InstructorClassPort;
import com.algoprep.problembank.domain.model.ClassAssignmentModels.ClassRef;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * DD F2 sections 53 and 54. Not transactional itself: each problem commits on its own in {@link
 * ClassAssignmentExecutor}, so one refused or failed problem never blocks the others.
 */
@Service
class ClassAssignmentCommandHandler implements ManageClassAssignmentsUseCase {
  static final int MAX_IDS = 50;

  private final InstructorClassPort classes;
  private final ClassAssignmentExecutor executor;

  ClassAssignmentCommandHandler(InstructorClassPort classes, ClassAssignmentExecutor executor) {
    this.classes = classes;
    this.executor = executor;
  }

  @Override
  public AssignResult assign(UUID instructorId, UUID classId, List<UUID> problemIds) {
    if (problemIds == null
        || problemIds.isEmpty()
        || problemIds.size() > MAX_IDS
        || problemIds.stream().anyMatch(java.util.Objects::isNull)) {
      throw new InvalidRequestException("PRB-001", "problemIds must hold 1 to 50 ids");
    }
    requireOwned(instructorId, classId);
    var succeeded = new ArrayList<UUID>();
    var skipped = new ArrayList<UUID>();
    var failed = new ArrayList<Failed>();
    for (UUID id : new LinkedHashSet<>(problemIds)) {
      try {
        (executor.assign(instructorId, classId, id) ? succeeded : skipped).add(id);
      } catch (AlgoPrepException refused) {
        failed.add(new Failed(id, refused.errorCode(), refused.getMessage()));
      }
    }
    return new AssignResult(succeeded, skipped, failed);
  }

  @Override
  public void remove(UUID instructorId, UUID classId, UUID problemId) {
    requireOwned(instructorId, classId);
    executor.remove(instructorId, classId, problemId);
  }

  private void requireOwned(UUID instructorId, UUID classId) {
    boolean owned =
        classes.classesOf(instructorId).stream().map(ClassRef::id).anyMatch(classId::equals);
    if (!owned) {
      throw new PermissionDeniedException("PRB-005", "You are not responsible for this class");
    }
  }
}
