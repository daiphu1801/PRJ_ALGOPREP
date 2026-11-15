package com.algoprep.identity.application.command;

import com.algoprep.common.exception.ResourceNotFoundException;
import com.algoprep.identity.domain.exception.IdentityErrorCodes;

/** One answer for "no such class" and "not your class", so the reply cannot probe other classes. */
final class OwnedClasses {
  private OwnedClasses() {}

  static ResourceNotFoundException notFound() {
    return new ResourceNotFoundException(
        IdentityErrorCodes.CLASS_NOT_FOUND, "Class is not available");
  }
}
