package com.algoprep.identity.application.ports.in.classes;

import com.algoprep.identity.domain.model.classes.MyClass;
import java.util.UUID;

/** Reads one class the caller currently belongs to (#25B). */
public interface GetMyClassUseCase {
  MyClass get(UUID userId, String classId);
}
