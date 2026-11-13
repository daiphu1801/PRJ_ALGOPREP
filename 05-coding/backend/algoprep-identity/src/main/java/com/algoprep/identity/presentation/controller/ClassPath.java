package com.algoprep.identity.presentation.controller;

import com.algoprep.common.exception.ResourceNotFoundException;
import com.algoprep.identity.domain.exception.IdentityErrorCodes;
import java.util.UUID;

/** A malformed id in a class path is the same 404 IDT-414 as a missing or foreign class. */
final class ClassPath {
  private ClassPath() {}

  static UUID id(String raw) {
    try {
      return UUID.fromString(raw);
    } catch (IllegalArgumentException e) {
      throw new ResourceNotFoundException(
          IdentityErrorCodes.CLASS_NOT_FOUND, "Class is not available");
    }
  }
}
