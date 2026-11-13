package com.algoprep.identity.application.dto;

import java.util.UUID;

/** Request #12: the six-digit code mailed to the new address. Excluded from diagnostics. */
public record ConfirmEmailChangeCommand(UUID userId, String code) {
  @Override
  public String toString() {
    return "ConfirmEmailChangeCommand[redacted]";
  }
}
