package com.algoprep.identity.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.nimbusds.jose.jwk.RSAKey;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.time.Instant;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;

/** JWT key loading (BD security): explicit PEM pair, dev fallback, prod fail-fast, bad input. */
class JwtKeyConfigTest {
  private final JwtKeyConfig config = new JwtKeyConfig();

  private static String pem(String label, byte[] der) {
    return "-----BEGIN "
        + label
        + "-----\n"
        + Base64.getMimeEncoder().encodeToString(der)
        + "\n-----END "
        + label
        + "-----";
  }

  private static String[] keyPair() throws Exception {
    KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
    generator.initialize(2048);
    KeyPair pair = generator.generateKeyPair();
    return new String[] {
      pem("PRIVATE KEY", pair.getPrivate().getEncoded()),
      pem("PUBLIC KEY", pair.getPublic().getEncoded())
    };
  }

  @Test
  void missingKeysGenerateEphemeralKeyOutsideProd() {
    RSAKey key = config.jwtSigningKey("", "", new MockEnvironment());
    assertThat(key.isPrivate()).isTrue();
    assertThat(key.getKeyID()).isNotBlank();
  }

  @Test
  void missingKeysAreRefusedInProdProfile() {
    MockEnvironment prod = new MockEnvironment();
    prod.setActiveProfiles("prod");
    assertThatThrownBy(() -> config.jwtSigningKey("", "", prod))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("prod profile");
  }

  @Test
  void onlyOneHalfOfThePairIsRefused() throws Exception {
    String[] pair = keyPair();
    assertThatThrownBy(() -> config.jwtSigningKey(pair[0], "", new MockEnvironment()))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("both");
    assertThatThrownBy(() -> config.jwtSigningKey("", pair[1], new MockEnvironment()))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  void garbagePemIsWrappedInIllegalState() {
    assertThatThrownBy(() -> config.jwtSigningKey("AAAA", "BBBB", new MockEnvironment()))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Cannot load JWT signing key");
  }

  @Test
  void explicitPairHasStableKeyIdAndEncoderDecoderRoundTrip() throws Exception {
    String[] pair = keyPair();
    RSAKey first = config.jwtSigningKey(pair[0], pair[1], new MockEnvironment());
    RSAKey second = config.jwtSigningKey(pair[0], pair[1], new MockEnvironment());
    assertThat(first.getKeyID()).isEqualTo(second.getKeyID());

    var encoder = config.jwtEncoder(first);
    var decoder = config.jwtDecoder(first);
    var claims =
        JwtClaimsSet.builder()
            .subject("user-1")
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(60))
            .build();
    var header =
        org.springframework.security.oauth2.jwt.JwsHeader.with(
                org.springframework.security.oauth2.jose.jws.SignatureAlgorithm.RS256)
            .build();
    String token = encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    assertThat(decoder.decode(token).getSubject()).isEqualTo("user-1");
  }

  @Test
  void decoderRejectsExpiredToken() throws Exception {
    RSAKey key = config.jwtSigningKey("", "", new MockEnvironment());
    var claims =
        JwtClaimsSet.builder()
            .subject("u")
            .issuedAt(Instant.now().minusSeconds(600))
            .expiresAt(Instant.now().minusSeconds(300))
            .build();
    var header =
        org.springframework.security.oauth2.jwt.JwsHeader.with(
                org.springframework.security.oauth2.jose.jws.SignatureAlgorithm.RS256)
            .build();
    String token =
        config.jwtEncoder(key).encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    assertThatThrownBy(() -> config.jwtDecoder(key).decode(token))
        .isInstanceOf(org.springframework.security.oauth2.jwt.JwtException.class);
  }
}
