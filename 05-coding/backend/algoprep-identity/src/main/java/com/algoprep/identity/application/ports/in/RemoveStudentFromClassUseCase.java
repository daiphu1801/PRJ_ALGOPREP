package com.algoprep.identity.application.ports.in;

import java.time.Instant;
import java.util.UUID;

/** Marks a student as left (#27) and returns the stored {@code left_at}; safe to repeat. */
public interface RemoveStudentFromClassUseCase {
  Instant remove(UUID instructorId, UUID classId, UUID studentId);
}
