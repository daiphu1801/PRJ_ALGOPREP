package com.algoprep.judge.presentation.controller;

import com.algoprep.common.exception.InvalidRequestException;
import com.algoprep.judge.domain.exception.MySubmissionMetricsUnavailableException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = MySubmissionMetricsController.class)
@Order(Ordered.HIGHEST_PRECEDENCE)
class MySubmissionMetricsErrorAdvice {
  @ExceptionHandler(MySubmissionMetricsUnavailableException.class)
  ProblemDetail unavailable() {
    var result =
        ProblemDetail.forStatusAndDetail(
            HttpStatusCode.valueOf(503), "Submission metrics unavailable");
    result.setProperty("errorCode", "JDG-507");
    return result;
  }

  @ExceptionHandler(InvalidRequestException.class)
  ProblemDetail invalid(InvalidRequestException failure) {
    var result =
        ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(400), "Invalid metrics request");
    result.setProperty("errorCode", failure.errorCode());
    return result;
  }

  @ExceptionHandler(AccessDeniedException.class)
  ProblemDetail denied() {
    return ProblemDetail.forStatusAndDetail(
        HttpStatusCode.valueOf(403), "Student metrics access denied");
  }
}
