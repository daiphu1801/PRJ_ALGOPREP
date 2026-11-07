package com.algoprep.identity.application.ports.in;

import com.algoprep.identity.domain.model.ClassStudentRow;
import java.util.List;
import java.util.UUID;

/** Lists the active students of an owned class (#26), with figures when they can be read. */
public interface ListClassRosterUseCase {
  List<ClassStudentRow> list(UUID instructorId, UUID classId);
}
