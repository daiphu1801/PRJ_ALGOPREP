package com.algoprep.bootstrap.identity;

import com.algoprep.identity.application.ports.out.ClassAssignmentLookupPort;
import com.algoprep.identity.domain.model.ClassProblemWindow;
import com.algoprep.problembank.application.ports.in.ListClassAssignmentsUseCase;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** Type translation only: identity asks, problem-bank answers through its inbound port. */
@Component
public final class ClassAssignmentLookupBridge implements ClassAssignmentLookupPort {
  private final ListClassAssignmentsUseCase assignments;

  public ClassAssignmentLookupBridge(ListClassAssignmentsUseCase assignments) {
    this.assignments = assignments;
  }

  @Override
  public List<ClassProblemWindow> assignmentsOf(Collection<UUID> classIds) {
    Set<UUID> requested = Set.copyOf(classIds);
    if (requested.isEmpty()) {
      return List.of();
    }
    try {
      return assignments.listAssignments(requested).stream()
          .map(
              row ->
                  new ClassProblemWindow(
                      row.classId(), row.problemId(), row.assignedAt(), row.removedAt()))
          .toList();
    } catch (RuntimeException failure) {
      throw new IllegalStateException("Class assignments are unavailable");
    }
  }
}
