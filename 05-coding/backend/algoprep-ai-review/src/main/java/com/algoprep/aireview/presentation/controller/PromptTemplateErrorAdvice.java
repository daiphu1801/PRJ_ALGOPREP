package com.algoprep.aireview.presentation.controller;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Method security throws before the controller runs, so this advice shapes AIR-301 and the
 * unreadable-body case. The other codes map through their base exceptions (AIR-201 409, AIR-202 and
 * AIR-205 422, AIR-503 503, AIR-V* 400).
 */
@RestControllerAdvice(assignableTypes = PromptTemplateController.class)
@Order(Ordered.HIGHEST_PRECEDENCE)
class PromptTemplateErrorAdvice {
  @ExceptionHandler(AccessDeniedException.class)
  ProblemDetail denied() {
    return error(HttpStatus.FORBIDDEN, "AIR-301", "AI configuration permission required");
  }

  /** Not JSON at all: the publish endpoint reports a bad list, the draft endpoint a bad text. */
  @ExceptionHandler(HttpMessageNotReadableException.class)
  ProblemDetail unreadable(jakarta.servlet.http.HttpServletRequest request) {
    String code = request.getRequestURI().endsWith("/publish") ? "AIR-V14" : "AIR-V12";
    return error(HttpStatus.BAD_REQUEST, code, "Request body is not readable JSON");
  }

  private static ProblemDetail error(HttpStatus status, String code, String message) {
    ProblemDetail detail = ProblemDetail.forStatusAndDetail(status, message);
    detail.setProperty("errorCode", code);
    return detail;
  }
}
