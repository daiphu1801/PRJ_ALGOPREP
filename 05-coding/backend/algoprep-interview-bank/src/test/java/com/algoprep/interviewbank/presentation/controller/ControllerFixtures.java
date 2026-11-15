package com.algoprep.interviewbank.presentation.controller;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Shared builders for the controller tests: a caller token, a request body and parsed JSON. */
final class ControllerFixtures {
  static final JsonMapper JSON = JsonMapper.builder().build();

  private ControllerFixtures() {}

  static JwtAuthenticationToken caller(UUID id, String... roles) {
    Jwt jwt = Jwt.withTokenValue("token").header("alg", "none").subject(id.toString()).build();
    List<SimpleGrantedAuthority> authorities =
        Arrays.stream(roles).map(SimpleGrantedAuthority::new).toList();
    return new JwtAuthenticationToken(jwt, authorities);
  }

  static MockHttpServletRequest request(String body) {
    var request = new MockHttpServletRequest();
    request.setContent(body.getBytes(StandardCharsets.UTF_8));
    return request;
  }

  static JsonNode json(String text) {
    return JSON.readTree(text);
  }
}
