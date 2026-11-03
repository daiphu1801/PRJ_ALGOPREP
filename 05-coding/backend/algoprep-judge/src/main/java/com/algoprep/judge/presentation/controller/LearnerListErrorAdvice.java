package com.algoprep.judge.presentation.controller;

import com.algoprep.common.exception.InvalidRequestException;
import com.algoprep.judge.domain.exception.JudgeReadUnavailableException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.TransactionException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Generic RFC9457 infrastructure response, without extending JDG-506/507. */
@RestControllerAdvice(
    assignableTypes = {
      LearnerSubmissionListsController.class,
      EnabledSubmissionLanguagesController.class
    })
@Order(Ordered.HIGHEST_PRECEDENCE)
class LearnerListErrorAdvice {
  @ExceptionHandler(InvalidRequestException.class)
  ProblemDetail invalid(InvalidRequestException failure) {
    var result = problem(400, "Invalid submission limit");
    result.setProperty("errorCode", failure.errorCode());
    return result;
  }

  @ExceptionHandler(AccessDeniedException.class)
  ProblemDetail denied() {
    return problem(403, "Submission list access denied");
  }

  @ExceptionHandler({
    JudgeReadUnavailableException.class,
    DataAccessException.class,
    TransactionException.class,
    IllegalStateException.class
  })
  ProblemDetail unavailable() {
    return problem(503, "Submission list temporarily unavailable");
  }

  private static ProblemDetail problem(int status, String detail) {
    return ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(status), detail);
  }
}
