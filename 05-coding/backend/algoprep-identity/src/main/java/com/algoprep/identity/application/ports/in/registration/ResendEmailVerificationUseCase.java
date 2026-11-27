package com.algoprep.identity.application.ports.in.registration;

/** Email confirmation resend, endpoint #54. */
public interface ResendEmailVerificationUseCase {
  void resend(String email);
}
