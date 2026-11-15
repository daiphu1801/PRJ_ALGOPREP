package com.algoprep.identity.application.ports.in;

import com.algoprep.identity.domain.model.ClassStudentRow;
import java.util.UUID;

/** One active student of an owned class with the judge figures (#28). */
public interface GetClassStudentProfileUseCase {
  ClassStudentRow get(UUID instructorId, UUID classId, UUID studentId);
}
