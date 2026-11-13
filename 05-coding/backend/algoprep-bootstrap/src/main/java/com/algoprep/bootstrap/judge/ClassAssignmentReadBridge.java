package com.algoprep.bootstrap.judge;

import com.algoprep.judge.application.dto.ClassAssignment;
import com.algoprep.judge.application.ports.out.ClassAssignmentReadPort;
import com.algoprep.problembank.application.ports.in.ListClassAssignmentsUseCase;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** Type translation only. This trusted read never grants instructor/class permissions. */
@Component
public final class ClassAssignmentReadBridge implements ClassAssignmentReadPort {
  private final ListClassAssignmentsUseCase assignments;

  public ClassAssignmentReadBridge(ListClassAssignmentsUseCase assignments) {
    this.assignments = assignments;
  }

  @Override
  public List<ClassAssignment> listAssignments(Collection<UUID> classIds) {
    Set<UUID> requested = Set.copyOf(classIds);
    if (requested.isEmpty()) {
      return List.of();
    }
    try {
      return assignments.listAssignments(requested).stream()
          .map(
              row ->
                  new ClassAssignment(
                      row.classId(),
                      row.problemId(),
                      row.problemTitle(),
                      row.assignedAt(),
                      row.removedAt()))
          .toList();
    } catch (RuntimeException failure) {
      throw new IllegalStateException("Class assignments are unavailable");
    }
  }
}
