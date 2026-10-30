package com.algoprep.identity.infrastructure.security;

import com.algoprep.common.domain.ports.out.ClockPort;
import com.algoprep.identity.application.dto.IssuedAccessToken;
import com.algoprep.identity.application.ports.out.AccessTokenPort;
import com.algoprep.identity.domain.model.BaseCategory;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

/**
 * Signs access tokens with RS256. Claims are exactly {@code sub}, {@code base_category}, {@code
 * role_code}, {@code iat}, {@code exp} (BD security section 1): no permission matrix in the token.
 */
@Component
class NimbusAccessTokenAdapter implements AccessTokenPort {

  static final String CLAIM_BASE_CATEGORY = "base_category";
  static final String CLAIM_ROLE_CODE = "role_code";

  private final JwtEncoder encoder;
  private final ClockPort clock;
  private final Duration ttl;

  NimbusAccessTokenAdapter(
      JwtEncoder encoder,
      ClockPort clock,
      @Value("${algoprep.security.jwt.access-ttl-seconds:900}") long ttlSeconds) {
    this.encoder = encoder;
    this.clock = clock;
    this.ttl = Duration.ofSeconds(ttlSeconds);
  }

  @Override
  public IssuedAccessToken issue(UUID userId, BaseCategory baseCategory, String roleCode) {
    Instant issuedAt = clock.now();
    Instant expiresAt = issuedAt.plus(ttl);
    JwtClaimsSet claims =
        JwtClaimsSet.builder()
            .subject(userId.toString())
            .claim(CLAIM_BASE_CATEGORY, baseCategory.name())
            .claim(CLAIM_ROLE_CODE, roleCode)
            .issuedAt(issuedAt)
            .expiresAt(expiresAt)
            .build();
    JwsHeader header = JwsHeader.with(SignatureAlgorithm.RS256).build();
    String token = encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    return new IssuedAccessToken(token, expiresAt);
  }
}
