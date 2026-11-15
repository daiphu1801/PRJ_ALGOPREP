package com.algoprep.problembank.application.ports.in.assignment;

import com.algoprep.problembank.domain.model.assignment.ClassAssignmentSnapshot;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/** Internal history read. Callers authorize class scope; removed/withdrawn rows remain visible. */
public interface ListClassAssignmentsUseCase {
  List<ClassAssignmentSnapshot> listAssignments(Collection<UUID> classIds);
}
