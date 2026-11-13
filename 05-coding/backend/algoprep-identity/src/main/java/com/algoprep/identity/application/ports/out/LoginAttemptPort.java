package com.algoprep.identity.application.ports.out;

/** BR-01 counter for a normalized email and server-observed source IP. */
public interface LoginAttemptPort {
  boolean isBlocked(String email, String sourceIp);

  void recordFailure(String email, String sourceIp);

  void clear(String email, String sourceIp);
}
