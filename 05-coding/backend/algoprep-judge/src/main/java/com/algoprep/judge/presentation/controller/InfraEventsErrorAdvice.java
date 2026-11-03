package com.algoprep.judge.presentation.controller;

import java.time.DateTimeException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.TransactionException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = InfraEventsController.class)
@Order(Ordered.HIGHEST_PRECEDENCE)
class InfraEventsErrorAdvice {
  @ExceptionHandler(AccessDeniedException.class)
  ProblemDetail denied() {
    var problem =
        ProblemDetail.forStatusAndDetail(
            HttpStatusCode.valueOf(403), "Infrastructure events access denied");
    problem.setProperty("errorCode", "JDG-301");
    return problem;
  }

  @ExceptionHandler({IllegalArgumentException.class, DateTimeException.class})
  ProblemDetail invalid() {
    var problem =
        ProblemDetail.forStatusAndDetail(
            HttpStatusCode.valueOf(400), "Invalid infrastructure event filters");
    problem.setProperty("errorCode", "JDG-V13");
    return problem;
  }

  @ExceptionHandler({
    DataAccessException.class,
    TransactionException.class,
    IllegalStateException.class
  })
  ProblemDetail unavailable() {
    return ProblemDetail.forStatusAndDetail(
        HttpStatusCode.valueOf(503), "Infrastructure events temporarily unavailable");
  }
}
