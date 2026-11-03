package com.algoprep.judge.presentation.controller;

import com.algoprep.judge.domain.exception.SampleRunException;
import com.algoprep.judge.domain.exception.SubmissionAdmissionException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = RunSampleController.class)
@Order(Ordered.HIGHEST_PRECEDENCE)
class RunSampleErrorAdvice {
  @ExceptionHandler(SubmissionAdmissionException.class)
  ProblemDetail invalid(SubmissionAdmissionException error) {
    int status =
        switch (error.errorCode()) {
          case "JDG-101" -> 404;
          case "JDG-102", "JDG-103", "JDG-105" -> 422;
          default -> 400;
        };
    var response =
        ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(status), "Sample request rejected");
    response.setProperty("errorCode", error.errorCode());
    return response;
  }

  @ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException.class)
  ProblemDetail malformed() {
    var response =
        ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(400), "Sample request rejected");
    response.setProperty("errorCode", "JDG-V01");
    return response;
  }

  @ExceptionHandler(SampleRunException.class)
  ProblemDetail unavailable(SampleRunException error) {
    int status = error.errorCode().equals("JDG-501") ? 503 : 429;
    var response =
        ProblemDetail.forStatusAndDetail(
            HttpStatusCode.valueOf(status), "Sample execution unavailable");
    response.setProperty("errorCode", error.errorCode());
    if (!error.clientErrorDetails().isEmpty()) {
      response.setProperty("details", error.clientErrorDetails());
    }
    return response;
  }
}
