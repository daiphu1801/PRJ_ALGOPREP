package com.algoprep.identity.application.ports.in;

/** Email confirmation resend, endpoint #54. */
public interface ResendEmailVerificationUseCase {
  void resend(String email);
}
