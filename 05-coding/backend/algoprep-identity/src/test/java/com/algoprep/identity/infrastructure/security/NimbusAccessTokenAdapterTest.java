package com.algoprep.identity.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.algoprep.common.domain.ports.out.ClockPort;
import com.algoprep.identity.application.dto.IssuedAccessToken;
import com.algoprep.identity.domain.model.BaseCategory;
import com.nimbusds.jose.jwk.RSAKey;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;

class NimbusAccessTokenAdapterTest {

  private static final long TTL_SECONDS = 900;

  private final Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
  private final ClockPort clock = () -> now;

  private JwtDecoder decoder;
  private NimbusAccessTokenAdapter adapter;

  @BeforeEach
  void setUp() throws Exception {
    JwtKeyConfig config = new JwtKeyConfig();
    RSAKey key = config.jwtSigningKey("", "");
    decoder = config.jwtDecoder(key);
    adapter = new NimbusAccessTokenAdapter(config.jwtEncoder(key), clock, TTL_SECONDS);
  }

  @Test
  @DisplayName(
      "token carries exactly sub, base_category, role_code, iat, exp and expires in 15 minutes")
  void claimsAreIdentityOnly() {
    UUID userId = UUID.randomUUID();

    IssuedAccessToken issued = adapter.issue(userId, BaseCategory.INSTRUCTOR, "INSTRUCTOR");
    Jwt jwt = decoder.decode(issued.value());

    assertThat(jwt.getClaims().keySet())
        .isEqualTo(Set.of("sub", "base_category", "role_code", "iat", "exp"));
    assertThat(jwt.getSubject()).isEqualTo(userId.toString());
    assertThat(jwt.<String>getClaim("base_category")).isEqualTo("INSTRUCTOR");
    assertThat(jwt.<String>getClaim("role_code")).isEqualTo("INSTRUCTOR");
    assertThat(Duration.between(jwt.getIssuedAt(), jwt.getExpiresAt()))
        .isEqualTo(Duration.ofMinutes(15));
    assertThat(issued.expiresAt()).isEqualTo(now.plusSeconds(TTL_SECONDS));
  }

  @Test
  @DisplayName("a token signed by another key is rejected")
  void foreignKeyIsRejected() throws Exception {
    JwtKeyConfig other = new JwtKeyConfig();
    RSAKey otherKey = other.jwtSigningKey("", "");
    NimbusAccessTokenAdapter foreign =
        new NimbusAccessTokenAdapter(other.jwtEncoder(otherKey), clock, TTL_SECONDS);

    String forged = foreign.issue(UUID.randomUUID(), BaseCategory.ADMIN, "ADMIN").value();

    assertThatThrownBy(() -> decoder.decode(forged)).isInstanceOf(JwtException.class);
  }

  @Test
  @DisplayName("an expired token is rejected")
  void expiredTokenIsRejected() throws Exception {
    JwtKeyConfig config = new JwtKeyConfig();
    RSAKey key = config.jwtSigningKey("", "");
    ClockPort past = () -> now.minus(Duration.ofHours(1));
    NimbusAccessTokenAdapter old =
        new NimbusAccessTokenAdapter(config.jwtEncoder(key), past, TTL_SECONDS);
    String token = old.issue(UUID.randomUUID(), BaseCategory.STUDENT, "STUDENT").value();

    assertThatThrownBy(() -> config.jwtDecoder(key).decode(token)).isInstanceOf(JwtException.class);
  }

  @Test
  @DisplayName("setting only one of the two PEM properties fails fast")
  void halfConfiguredKeyFails() {
    assertThatThrownBy(() -> new JwtKeyConfig().jwtSigningKey("pem", ""))
        .isInstanceOf(IllegalStateException.class);
  }
}
