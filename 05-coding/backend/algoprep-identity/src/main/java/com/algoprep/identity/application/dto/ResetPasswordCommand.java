package com.algoprep.identity.application.dto;

/** Secrets are excluded from diagnostics. */
public record ResetPasswordCommand(String email, String otp, String newPassword) {
  @Override
  public String toString() {
    return "ResetPasswordCommand[redacted]";
  }
}
