package com.algoprep.problembank.application.ports.out;

import com.algoprep.problembank.domain.model.ClassAssignmentModels.ClassRef;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** The classes a student is currently enrolled in, from the identity module. */
public interface StudentEnrollmentPort {
  /** Empty when identity cannot answer in time; an empty list means "enrolled in no class". */
  Optional<List<ClassRef>> activeClassesOf(UUID studentId);
}
