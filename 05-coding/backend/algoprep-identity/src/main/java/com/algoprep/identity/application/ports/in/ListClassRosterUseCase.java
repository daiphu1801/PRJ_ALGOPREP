package com.algoprep.identity.application.ports.in;

import com.algoprep.identity.domain.model.ClassRosterEntry;
import java.util.List;
import java.util.UUID;

/** Lists the active students of an owned class (#26). */
public interface ListClassRosterUseCase {
  List<ClassRosterEntry> list(UUID instructorId, UUID classId);
}
