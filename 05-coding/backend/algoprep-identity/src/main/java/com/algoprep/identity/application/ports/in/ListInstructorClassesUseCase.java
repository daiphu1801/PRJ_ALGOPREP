package com.algoprep.identity.application.ports.in;

import com.algoprep.identity.domain.model.InstructorClass;
import java.util.List;
import java.util.UUID;

/** Lists the classes of the calling instructor (#19). */
public interface ListInstructorClassesUseCase {
  List<InstructorClass> list(UUID instructorId);
}
