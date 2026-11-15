package com.algoprep.identity.application.dto;

/** No OTP value in diagnostics. */
public record VerifyPasswordResetOtpCommand(String email, String otp) {
  @Override
  public String toString() {
    return "VerifyPasswordResetOtpCommand[redacted]";
  }
}
