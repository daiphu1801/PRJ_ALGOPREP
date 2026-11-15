package com.algoprep.identity.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RefreshTokenSecretsTest {

  @Test
  @DisplayName("raw token is URL-safe, 256 bits, and different every call")
  void rawTokenIsRandomAndUrlSafe() {
    String first = RefreshTokenSecrets.newRawToken();
    String second = RefreshTokenSecrets.newRawToken();

    assertThat(first).isNotEqualTo(second);
    assertThat(first).hasSize(43).matches("[A-Za-z0-9_-]+");
  }

  @Test
  @DisplayName("hash is the known SHA-256 hex of the UTF-8 bytes")
  void hashMatchesKnownVector() {
    assertThat(RefreshTokenSecrets.hash("abc"))
        .isEqualTo("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad");
  }

  @Test
  @DisplayName("hash is stable for the same input and differs for different input")
  void hashIsDeterministic() {
    assertThat(RefreshTokenSecrets.hash("a")).isEqualTo(RefreshTokenSecrets.hash("a"));
    assertThat(RefreshTokenSecrets.hash("a")).isNotEqualTo(RefreshTokenSecrets.hash("b"));
  }
}
