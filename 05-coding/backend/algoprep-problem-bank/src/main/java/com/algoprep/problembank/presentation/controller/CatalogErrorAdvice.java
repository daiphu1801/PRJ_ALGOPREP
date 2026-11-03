package com.algoprep.problembank.presentation.controller;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(
    assignableTypes = {ProblemLevelsController.class, ProblemTopicsController.class})
@Order(Ordered.HIGHEST_PRECEDENCE)
class CatalogErrorAdvice {
  @ExceptionHandler(AccessDeniedException.class)
  ProblemDetail denied() {
    return error(403, "PRB-002", "Problem authoring permission required");
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  ProblemDetail invalidBody() {
    return error(400, "PRB-001", "Invalid catalogue request");
  }

  private ProblemDetail error(int status, String code, String message) {
    var detail =
        ProblemDetail.forStatusAndDetail(
            org.springframework.http.HttpStatusCode.valueOf(status), message);
    detail.setProperty("errorCode", code);
    return detail;
  }
}
