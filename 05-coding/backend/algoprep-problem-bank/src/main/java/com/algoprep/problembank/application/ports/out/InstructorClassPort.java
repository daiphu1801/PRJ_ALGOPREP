package com.algoprep.problembank.application.ports.out;

import com.algoprep.problembank.domain.model.ClassAssignmentModels.ClassRef;
import java.util.List;
import java.util.UUID;

/** The classes an instructor is responsible for, from the identity module. */
public interface InstructorClassPort {
  /** Throws a 503 {@code PRB-058} when identity cannot answer: ownership must never be guessed. */
  List<ClassRef> classesOf(UUID instructorId);
}
