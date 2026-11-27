package com.algoprep.judge.presentation.controller;

import com.algoprep.judge.domain.exception.ProblemGradingReadException;
import com.algoprep.judge.domain.exception.QueueFailureFinalizationUnavailableException;
import com.algoprep.judge.domain.exception.SubmissionAdmissionException;
import com.algoprep.judge.domain.exception.SubmissionAdmissionUnavailableException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Endpoint-local mapping leaves the shared identity and global error handlers unchanged. */
@RestControllerAdvice(assignableTypes = CreateSubmissionController.class)
@Order(Ordered.HIGHEST_PRECEDENCE)
class CreateSubmissionErrorAdvice {
  @ExceptionHandler(SubmissionAdmissionException.class)
  ProblemDetail admission(SubmissionAdmissionException failure) {
    int status =
        switch (failure.errorCode()) {
          case "JDG-101" -> 404;
          case "JDG-102", "JDG-103", "JDG-104" -> 422;
          case "JDG-106" -> 429;
          case "JDG-503", "JDG-504" -> 503;
          default -> 400;
        };
    var detail = problem(status, failure.errorCode());
    if (!failure.clientErrorDetails().isEmpty()) {
      detail.setProperty("details", failure.clientErrorDetails());
    }
    return detail;
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  ProblemDetail unreadable() {
    return problem(400, "JDG-V01");
  }

  @ExceptionHandler({
    SubmissionAdmissionUnavailableException.class,
    QueueFailureFinalizationUnavailableException.class,
    ProblemGradingReadException.class
  })
  ProblemDetail unavailable() {
    // No queue-failure claim: the transaction outcome may be unknown to this caller.
    return problem(503, "JDG-505");
  }

  private static ProblemDetail problem(int status, String code) {
    var detail =
        ProblemDetail.forStatusAndDetail(
            HttpStatusCode.valueOf(status), "Submission request could not be completed");
    detail.setProperty("errorCode", code);
    return detail;
  }
}
