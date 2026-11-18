package com.algoprep.judge.presentation.controller;

import com.algoprep.judge.domain.exception.AdminConfigException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.TransactionException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = AdminLanguageConfigController.class)
@Order(Ordered.HIGHEST_PRECEDENCE)
class AdminLanguageConfigErrorAdvice {
  @ExceptionHandler(AdminConfigException.class)
  ProblemDetail invalid(AdminConfigException failure) {
    return problem(failure.status(), failure.errorCode());
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  ProblemDetail malformed() {
    return problem(400, "JDG-V10");
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
            status == 503
                ? "Judge configuration temporarily unavailable"
                : "Judge configuration request rejected");
    if (code != null) {
      result.setProperty("errorCode", code);
    }
    return result;
  }
}
