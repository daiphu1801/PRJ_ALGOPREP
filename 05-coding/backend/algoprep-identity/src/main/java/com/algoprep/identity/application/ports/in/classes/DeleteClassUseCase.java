package com.algoprep.identity.application.ports.in.classes;

import java.util.UUID;

/** Hard-deletes an owned class and announces {@code ClassDeleted} after commit (#22). */
public interface DeleteClassUseCase {
  void delete(UUID instructorId, UUID classId);
}
