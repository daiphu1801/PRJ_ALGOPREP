package com.algoprep.judge.presentation.controller;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Generic infrastructure errors never serialize database or metadata exception messages. */
@RestControllerAdvice(
    assignableTypes = {MySubmissionReadController.class, InstructorSubmissionReadController.class})
@Order(Ordered.HIGHEST_PRECEDENCE)
class SubmissionReadErrorAdvice {
  @ExceptionHandler(AccessDeniedException.class)
  ProblemDetail forbidden() {
    return problem(403, "JDG-301");
  }

  @ExceptionHandler({
    org.springframework.dao.DataAccessException.class,
    org.springframework.transaction.CannotCreateTransactionException.class,
    org.springframework.transaction.TransactionTimedOutException.class,
    IllegalStateException.class
  })
  ProblemDetail unavailable() {
    return problem(503, com.algoprep.judge.domain.exception.SubmissionReadErrorCodes.UNAVAILABLE);
  }

  private static ProblemDetail problem(int status, String code) {
    var problem =
        ProblemDetail.forStatusAndDetail(
            HttpStatusCode.valueOf(status), "Submission read unavailable");
    problem.setProperty("errorCode", code);
    return problem;
  }
}
