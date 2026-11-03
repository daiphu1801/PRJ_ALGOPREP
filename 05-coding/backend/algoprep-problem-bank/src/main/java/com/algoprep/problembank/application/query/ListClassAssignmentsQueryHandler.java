package com.algoprep.problembank.application.query;

import com.algoprep.problembank.application.ports.in.ListClassAssignmentsUseCase;
import com.algoprep.problembank.domain.model.ClassAssignmentSnapshot;
import com.algoprep.problembank.domain.ports.out.ClassAssignmentReadRepository;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class ListClassAssignmentsQueryHandler implements ListClassAssignmentsUseCase {
  private final ClassAssignmentReadRepository assignments;

  ListClassAssignmentsQueryHandler(ClassAssignmentReadRepository assignments) {
    this.assignments = assignments;
  }

  @Override
  @Transactional(readOnly = true)
  public List<ClassAssignmentSnapshot> listAssignments(Collection<UUID> classIds) {
    Set<UUID> requested = Set.copyOf(classIds);
    return requested.isEmpty() ? List.of() : List.copyOf(assignments.findAssignments(requested));
  }
}
