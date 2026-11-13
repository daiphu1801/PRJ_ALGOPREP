package com.algoprep.judge.presentation.controller;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.TransactionException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = QueueMonitorController.class)
@Order(Ordered.HIGHEST_PRECEDENCE)
class QueueMonitorErrorAdvice {
  @ExceptionHandler(AccessDeniedException.class)
  ProblemDetail denied() {
    var problem =
        ProblemDetail.forStatusAndDetail(
            HttpStatusCode.valueOf(403), "Queue monitor access denied");
    problem.setProperty("errorCode", "JDG-301");
    return problem;
  }

  @ExceptionHandler({
    DataAccessException.class,
    TransactionException.class,
    IllegalStateException.class
  })
  ProblemDetail unavailable() {
    return ProblemDetail.forStatusAndDetail(
        HttpStatusCode.valueOf(503), "Queue monitor temporarily unavailable");
  }
}
