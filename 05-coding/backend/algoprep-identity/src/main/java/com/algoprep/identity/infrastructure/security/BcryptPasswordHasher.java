package com.algoprep.identity.infrastructure.security;

import com.algoprep.identity.application.ports.out.PasswordHasherPort;
import com.algoprep.identity.domain.model.PasswordPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

/** BCrypt with Spring Security's default strength (BD security section 1). */
@Component
class BcryptPasswordHasher implements PasswordHasherPort {

  private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

  /** Hash of a throwaway value, so a missing account costs the same as a wrong password. */
  private final String dummyHash = encoder.encode("not-a-real-password");

  @Override
  public String encode(String rawPassword) {
    return encoder.encode(rawPassword);
  }

  @Override
  public boolean matches(String rawPassword, String hashOrNull) {
    if (!PasswordPolicy.fitsBcrypt(rawPassword)) {
      // Keep a BCrypt check even for rejected input, regardless of account existence.
      encoder.matches("not-a-real-password", dummyHash);
      return false;
    }
    if (hashOrNull == null) {
      encoder.matches(rawPassword, dummyHash);
      return false;
    }
    return encoder.matches(rawPassword, hashOrNull);
  }
}
