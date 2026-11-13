package com.algoprep.problembank.domain.ports.out;

import java.util.UUID;

public interface ClassDeletedRepository {
  int removeAssignments(UUID classId);
}
