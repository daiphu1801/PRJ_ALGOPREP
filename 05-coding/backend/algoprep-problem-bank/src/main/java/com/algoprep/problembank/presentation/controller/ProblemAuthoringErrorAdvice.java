package com.algoprep.problembank.presentation.controller;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/** A missing cell is 403 PRB-002 and an unreadable body or parameter is 400 PRB-001 (DD F2 1.2). */
@RestControllerAdvice(
    assignableTypes = {
      ProblemAuthoringController.class,
      ProblemAuthoringHelpersController.class,
      ProblemAuthoringListController.class,
      ProblemAuthoringBulkController.class,
      ProblemTestcaseController.class,
      ProblemTestcaseBulkController.class,
      ClassAssignmentController.class
    })
@Order(Ordered.HIGHEST_PRECEDENCE)
class ProblemAuthoringErrorAdvice {
  @ExceptionHandler(AccessDeniedException.class)
  ProblemDetail denied() {
    return error(HttpStatus.FORBIDDEN, "PRB-002", "Problem authoring permission required");
  }

  /** A query parameter of the wrong type, for example {@code pageSize=abc}. */
  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  ProblemDetail invalidParameter() {
    return error(HttpStatus.BAD_REQUEST, "PRB-001", "Invalid problem request");
  }

  /** A bound parameter that is not a number, for example {@code pageSize=abc}. */
  @ExceptionHandler(BindException.class)
  ProblemDetail unbindable() {
    return error(HttpStatus.BAD_REQUEST, "PRB-001", "Invalid problem request");
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  ProblemDetail invalidBody() {
    return error(HttpStatus.BAD_REQUEST, "PRB-001", "Invalid problem request");
  }

  private static ProblemDetail error(HttpStatus status, String code, String message) {
    var detail = ProblemDetail.forStatusAndDetail(status, message);
    detail.setProperty("errorCode", code);
    return detail;
  }
}
