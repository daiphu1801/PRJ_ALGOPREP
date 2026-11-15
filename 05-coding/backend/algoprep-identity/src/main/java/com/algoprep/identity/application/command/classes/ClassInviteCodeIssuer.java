package com.algoprep.identity.application.command;

import com.algoprep.common.domain.ports.out.IdGeneratorPort;
import com.algoprep.identity.domain.model.ClassInviteCode;
import com.algoprep.identity.domain.ports.out.InstructorClassRepository;
import java.security.SecureRandom;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Draws a code of 8 characters without the look-alikes 0, O, 1 and I. A clash with an existing code
 * is rare (32^8 values), so a few redraws are enough; the table's unique key is the real guard.
 */
@Component
class ClassInviteCodeIssuer {
  static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
  private static final int LENGTH = 8;
  private static final int ATTEMPTS = 5;

  private final InstructorClassRepository classes;
  private final IdGeneratorPort ids;
  private final SecureRandom random = new SecureRandom();

  ClassInviteCodeIssuer(InstructorClassRepository classes, IdGeneratorPort ids) {
    this.classes = classes;
    this.ids = ids;
  }

  ClassInviteCode issue(UUID classId, UUID createdBy) {
    for (int i = 0; i < ATTEMPTS; i++) {
      Optional<ClassInviteCode> stored =
          classes.insertInviteCode(ids.newId(), classId, createdBy, randomCode());
      if (stored.isPresent()) {
        return stored.get();
      }
    }
    throw new IllegalStateException("Could not draw a free invite code");
  }

  private String randomCode() {
    StringBuilder code = new StringBuilder(LENGTH);
    for (int i = 0; i < LENGTH; i++) {
      code.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
    }
    return code.toString();
  }
}
