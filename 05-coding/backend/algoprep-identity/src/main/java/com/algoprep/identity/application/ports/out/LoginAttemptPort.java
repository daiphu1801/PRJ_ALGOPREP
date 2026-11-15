package com.algoprep.identity.application.ports.out;

/**
 * BR-01 counter for a normalized email and server-observed source IP. Every operation must fail
 * closed with a service-unavailable error when the protection cannot be checked or updated; callers
 * must not interpret infrastructure failure as permission to try another password.
 */
public interface LoginAttemptPort {
  boolean isBlocked(String email, String sourceIp);

  void recordFailure(String email, String sourceIp);

  void clear(String email, String sourceIp);
}
