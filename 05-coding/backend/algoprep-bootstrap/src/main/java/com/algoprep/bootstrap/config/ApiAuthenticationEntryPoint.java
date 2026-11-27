package com.algoprep.bootstrap.config;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint;
import org.springframework.security.web.AuthenticationEntryPoint;

/**
 * Bearer failures: 401 as before, except when the account or permission data could not be read from
 * the database. That is not the caller's fault, so it answers 503 {@code IDT-503} and the client
 * retries instead of dropping the session. The request is still rejected (fail closed, BD security
 * section 1).
 */
final class ApiAuthenticationEntryPoint implements AuthenticationEntryPoint {
  static final String UNAVAILABLE_CODE = "IDT-503";

  private static final String UNAVAILABLE_BODY =
      "{\"type\":\"about:blank\",\"title\":\"Service Unavailable\",\"status\":503,"
          + "\"detail\":\"Authorization data is temporarily unavailable\","
          + "\"errorCode\":\""
          + UNAVAILABLE_CODE
          + "\"}";

  private final AuthenticationEntryPoint bearer = new BearerTokenAuthenticationEntryPoint();

  @Override
  public void commence(
      HttpServletRequest request, HttpServletResponse response, AuthenticationException failure)
      throws IOException {
    if (failure.getCause() instanceof DataAccessException) {
      response.setStatus(HttpStatus.SERVICE_UNAVAILABLE.value());
      response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
      response.setCharacterEncoding(StandardCharsets.UTF_8.name());
      response.getWriter().write(UNAVAILABLE_BODY);
      return;
    }
    try {
      bearer.commence(request, response, failure);
    } catch (ServletException e) {
      throw new IOException(e);
    }
  }
}
