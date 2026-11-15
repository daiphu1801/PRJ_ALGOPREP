package com.algoprep.identity.application.dto.registration;

/** Verification secret is excluded from diagnostics. */
public record VerifyEmailCommand(String email, String otp) {
  @Override
  public String toString() {
    return "VerifyEmailCommand[redacted]";
  }
}
