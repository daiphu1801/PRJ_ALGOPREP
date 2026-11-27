package com.algoprep.judge.application.ports.out;

import com.algoprep.judge.application.dto.ClassAssignment;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Reads every interval for authorized class IDs, including removed assignments and withdrawn
 * problems. Infrastructure failures throw; an empty list means no assignments, never a fallback.
 */
public interface ClassAssignmentReadPort {
  List<ClassAssignment> listAssignments(Collection<UUID> classIds);
}
