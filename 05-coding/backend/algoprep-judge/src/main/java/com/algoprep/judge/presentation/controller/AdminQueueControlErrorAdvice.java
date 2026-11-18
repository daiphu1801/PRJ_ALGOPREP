package com.algoprep.judge.presentation.controller;

import com.algoprep.judge.domain.exception.AdminConfigException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.TransactionException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Queue-only RFC9457 failures; JDG-506/507 retain their approved read/metrics scopes. */
@RestControllerAdvice(assignableTypes = AdminQueueControlController.class)
@Order(Ordered.HIGHEST_PRECEDENCE)
class AdminQueueControlErrorAdvice {
  @ExceptionHandler(AdminConfigException.class)
  ProblemDetail invalid(AdminConfigException failure) {
    return problem(failure.status(), failure.errorCode());
  }

  @ExceptionHandler(AccessDeniedException.class)
  ProblemDetail denied() {
    return problem(403, "JDG-301");
  }

  @ExceptionHandler({
    DataAccessException.class,
    TransactionException.class,
    IllegalStateException.class
  })
  ProblemDetail unavailable() {
    return problem(503, null);
  }

  private static ProblemDetail problem(int status, String code) {
    var result =
        ProblemDetail.forStatusAndDetail(
            HttpStatusCode.valueOf(status),
            status == 503 ? "Queue controls temporarily unavailable" : "Queue request rejected");
    if (code != null) {
      result.setProperty("errorCode", code);
    }
    return result;
  }
}
