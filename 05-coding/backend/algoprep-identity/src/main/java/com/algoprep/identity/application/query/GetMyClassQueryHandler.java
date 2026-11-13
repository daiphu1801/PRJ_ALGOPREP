package com.algoprep.identity.application.query;

import com.algoprep.common.exception.ResourceNotFoundException;
import com.algoprep.identity.application.ports.in.GetMyClassUseCase;
import com.algoprep.identity.domain.exception.IdentityErrorCodes;
import com.algoprep.identity.domain.model.MyClass;
import com.algoprep.identity.domain.ports.out.StudentClassRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Malformed, missing, left and foreign classes all answer 404 IDT-411, never 403. */
@Service
class GetMyClassQueryHandler implements GetMyClassUseCase {
  private final StudentClassRepository classes;

  GetMyClassQueryHandler(StudentClassRepository classes) {
    this.classes = classes;
  }

  @Override
  @Transactional(readOnly = true)
  public MyClass get(UUID userId, String classId) {
    return parse(classId)
        .flatMap(id -> classes.findActive(userId, id))
        .orElseThrow(
            () ->
                new ResourceNotFoundException(
                    IdentityErrorCodes.MY_CLASS_NOT_FOUND, "Class is not available"));
  }

  private static Optional<UUID> parse(String raw) {
    if (raw == null) {
      return Optional.empty();
    }
    try {
      return Optional.of(UUID.fromString(raw));
    } catch (IllegalArgumentException ex) {
      return Optional.empty();
    }
  }
}
