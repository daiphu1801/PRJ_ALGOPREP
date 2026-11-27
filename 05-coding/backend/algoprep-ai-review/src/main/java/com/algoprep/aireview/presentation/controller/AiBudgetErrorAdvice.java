package com.algoprep.aireview.presentation.controller;

import jakarta.servlet.http.HttpServletRequest;
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
 * unreadable-body case. Other codes map through their base exceptions (AIR-203 422, AIR-204 409,
 * AIR-120 and AIR-121 404, AIR-503 503, AIR-V* 400).
 */
@RestControllerAdvice(
    assignableTypes = {
      TokenBudgetController.class,
      ModelPriceController.class,
      AiUsageController.class
    })
@Order(Ordered.HIGHEST_PRECEDENCE)
class AiBudgetErrorAdvice {
  @ExceptionHandler(AccessDeniedException.class)
  ProblemDetail denied() {
    return error(HttpStatus.FORBIDDEN, "AIR-301", "AI budget or configuration permission required");
  }

  /** A body that is not JSON takes the validation code of the endpoint it was sent to. */
  @ExceptionHandler(HttpMessageNotReadableException.class)
  ProblemDetail unreadable(HttpServletRequest request) {
    String uri = request.getRequestURI();
    String code = "AIR-V17";
    if (uri.endsWith("/token-budget/limit")) {
      code = "AIR-V15";
    } else if (uri.endsWith("/limits-config")) {
      code = "AIR-V16";
    }
    return error(HttpStatus.BAD_REQUEST, code, "Request body is not readable JSON");
  }

  private static ProblemDetail error(HttpStatus status, String code, String message) {
    ProblemDetail detail = ProblemDetail.forStatusAndDetail(status, message);
    detail.setProperty("errorCode", code);
    return detail;
  }
}
