package com.algoprep.problembank.domain.ports.out;

import com.algoprep.problembank.domain.model.ClassAssignmentSnapshot;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** All assignment intervals for exactly the requested classes, without publication filtering. */
public interface ClassAssignmentReadRepository {
  List<ClassAssignmentSnapshot> findAssignments(Set<UUID> classIds);
}
