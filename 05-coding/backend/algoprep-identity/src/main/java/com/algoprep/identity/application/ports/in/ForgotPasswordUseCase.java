package com.algoprep.identity.application.ports.in;

/** Sends reset OTP or OAuth guidance; the HTTP response remains generic (#7). */
public interface ForgotPasswordUseCase {
  void forgot(String email);
}
