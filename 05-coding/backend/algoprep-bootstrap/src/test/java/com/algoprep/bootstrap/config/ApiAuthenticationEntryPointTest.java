package com.algoprep.bootstrap.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;

class ApiAuthenticationEntryPointTest {
  private final ApiAuthenticationEntryPoint entryPoint = new ApiAuthenticationEntryPoint();

  @Test
  void unreadableAuthorizationDataIsA503WithAStableCode() throws Exception {
    MockHttpServletResponse response = new MockHttpServletResponse();

    entryPoint.commence(
        new MockHttpServletRequest(),
        response,
        new InvalidBearerTokenException(
            "unavailable", new DataAccessResourceFailureException("db down")));

    assertThat(response.getStatus()).isEqualTo(503);
    assertThat(response.getContentType()).startsWith("application/problem+json");
    assertThat(response.getContentAsString())
        .contains("\"status\":503")
        .contains("\"errorCode\":\"IDT-503\"");
  }

  @Test
  void anyOtherBearerFailureStaysA401WithTheBearerChallenge() throws Exception {
    MockHttpServletResponse response = new MockHttpServletResponse();

    entryPoint.commence(
        new MockHttpServletRequest(), response, new InvalidBearerTokenException("bad token"));

    assertThat(response.getStatus()).isEqualTo(401);
    assertThat(response.getHeader("WWW-Authenticate")).startsWith("Bearer");
    assertThat(response.getContentAsString()).doesNotContain("IDT-503");
  }
}
