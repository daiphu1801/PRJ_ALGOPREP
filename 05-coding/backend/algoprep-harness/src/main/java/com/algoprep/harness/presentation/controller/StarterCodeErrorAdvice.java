package com.algoprep.harness.presentation.controller;

import com.algoprep.common.exception.AlgoPrepException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.TransactionException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/** Local HTTP translation never returns upstream diagnostics or a partial starter. */
@RestControllerAdvice(assignableTypes = StarterCodeController.class)
@Order(Ordered.HIGHEST_PRECEDENCE)
class StarterCodeErrorAdvice {
  @ExceptionHandler(AlgoPrepException.class)
  ProblemDetail known(AlgoPrepException error) {
    int status =
        switch (error.errorCode()) {
          case "HRN-001" -> 404;
          case "HRN-004", "HRN-005" -> 422;
          case "HRN-010" -> 400;
          case "HRN-003" -> 503;
          default -> 500;
        };
    String code =
        switch (error.errorCode()) {
          case "HRN-001", "HRN-003", "HRN-004", "HRN-005", "HRN-006", "HRN-010", "HRN-030" ->
              error.errorCode();
          default -> "HRN-031";
        };
    return problem(status, code);
  }

  @ExceptionHandler({DataAccessException.class, TransactionException.class})
  ProblemDetail dependency() {
    return problem(503, "HRN-003");
  }

  @ExceptionHandler({
    MissingServletRequestParameterException.class,
    MethodArgumentTypeMismatchException.class
  })
  ProblemDetail binding() {
    return ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(400), "Invalid starter request");
  }

  @ExceptionHandler(AccessDeniedException.class)
  ProblemDetail forbidden() {
    return ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(403), "Starter access denied");
  }

  @ExceptionHandler(RuntimeException.class)
  ProblemDetail unexpected() {
    return problem(500, "HRN-031");
  }

  private static ProblemDetail problem(int status, String code) {
    var result =
        ProblemDetail.forStatusAndDetail(
            HttpStatusCode.valueOf(status), "Starter code unavailable");
    result.setProperty("errorCode", code);
    return result;
  }
}
