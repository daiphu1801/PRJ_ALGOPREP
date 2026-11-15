package com.algoprep.identity.domain.model;

/** Opaque secret returned only in the credential-verified IDT-104 response. */
public record RestorationTicket(String value, int expiresInSeconds) {
  @Override
  public String toString() {
    return "RestorationTicket[redacted]";
  }
}
