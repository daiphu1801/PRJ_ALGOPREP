package com.algoprep.bootstrap.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;

/**
 * Answers an authenticated request that lacks a required authority with {@code 403 IDT-445} in the
 * same RFC 9457 shape as {@code GlobalExceptionHandler} (DD api F1 section 0.5).
 *
 * <p>Method security throws {@link AccessDeniedException} before the controller runs, so it never
 * reaches {@code @RestControllerAdvice}; this handler is the single place that shapes it.
 */
final class ApiAccessDeniedHandler implements AccessDeniedHandler {

  private static final String BODY =
      "{\"type\":\"about:blank\",\"title\":\"Forbidden\",\"status\":403,"
          + "\"detail\":\"Missing required permission\",\"errorCode\":\"IDT-445\"}";

  @Override
  public void handle(
      HttpServletRequest request,
      HttpServletResponse response,
      AccessDeniedException accessDeniedException)
      throws IOException {
    response.setStatus(HttpStatus.FORBIDDEN.value());
    response.setContentType("application/problem+json");
    response.setCharacterEncoding(StandardCharsets.UTF_8.name());
    response.getWriter().write(BODY);
  }
}
