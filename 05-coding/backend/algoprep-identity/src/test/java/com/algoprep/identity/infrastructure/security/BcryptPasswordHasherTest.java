package com.algoprep.identity.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** A suffix beyond BCrypt's byte boundary must never authenticate as the original password. */
class BcryptPasswordHasherTest {
  private final BcryptPasswordHasher hasher = new BcryptPasswordHasher();

  @ParameterizedTest
  @ValueSource(strings = {"A", "é"})
  void maximumLengthPasswordRoundTripsButExtraSuffixIsRejected(String character) {
    int repetitions = character.equals("A") ? 70 : 35;
    String password = character.repeat(repetitions) + "A1";
    String hash = hasher.encode(password);
    assertThat(hasher.matches(password, hash)).isTrue();
    assertThat(hasher.matches(password + "x", hash)).isFalse();
    assertThat(hasher.matches(password + "x", null)).isFalse();
  }
}
