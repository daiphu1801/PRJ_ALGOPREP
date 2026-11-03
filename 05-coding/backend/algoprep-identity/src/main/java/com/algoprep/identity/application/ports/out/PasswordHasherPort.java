package com.algoprep.identity.application.ports.out;

/** Password hashing and checking (BCrypt in production). */
public interface PasswordHasherPort {

  /** Hashes a validated password for storage. */
  String encode(String rawPassword);

  /**
   * Checks a password. When {@code hashOrNull} is null the check still does the same amount of work
   * against a dummy hash and returns false, so response time does not reveal whether the account
   * exists or has a password.
   *
   * @param rawPassword what the user typed
   * @param hashOrNull stored hash, or null when there is none
   * @return true only if the password matches a real hash
   */
  boolean matches(String rawPassword, String hashOrNull);
}
