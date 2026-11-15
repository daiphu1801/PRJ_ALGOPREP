package com.algoprep.problembank.presentation.controller;

import java.util.List;
import java.util.UUID;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/**
 * Shared test helpers. The module has no servlet API on its test classpath, so controllers are
 * exercised by direct method calls with a real {@link JwtAuthenticationToken}; HTTP mapping
 * annotations are asserted by reflection where the contract depends on them.
 */
final class ControllerSupport {
  private ControllerSupport() {}

  static JwtAuthenticationToken jwt(UUID subject, String... authorities) {
    Jwt token =
        Jwt.withTokenValue("token").header("alg", "none").subject(subject.toString()).build();
    List<GrantedAuthority> granted =
        java.util.Arrays.stream(authorities)
            .<GrantedAuthority>map(SimpleGrantedAuthority::new)
            .toList();
    return new JwtAuthenticationToken(token, granted);
  }
}
