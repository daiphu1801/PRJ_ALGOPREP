package com.algoprep.problembank.presentation.controller;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = ProblemSpecController.class)
@Order(Ordered.HIGHEST_PRECEDENCE)
class ProblemSpecErrorAdvice {
  @ExceptionHandler(AccessDeniedException.class)
  ProblemDetail denied() {
    var detail =
        ProblemDetail.forStatusAndDetail(
            org.springframework.http.HttpStatus.FORBIDDEN, "Problem authoring permission required");
    detail.setProperty("errorCode", "PRB-002");
    return detail;
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  ProblemDetail invalidBody() {
    var detail =
        ProblemDetail.forStatusAndDetail(
            org.springframework.http.HttpStatus.BAD_REQUEST, "Invalid specification request");
    detail.setProperty("errorCode", "PRB-001");
    return detail;
  }
}
