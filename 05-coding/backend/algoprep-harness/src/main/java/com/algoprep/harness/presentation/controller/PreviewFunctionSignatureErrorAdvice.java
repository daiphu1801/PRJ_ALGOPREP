package com.algoprep.harness.presentation.controller;

import com.algoprep.harness.application.preview.PreviewBodyTooLargeException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** HTTP 413 has no global mapping; 422 (HRN-060) and 429 (HRN-061) use the shared handler. */
@RestControllerAdvice(assignableTypes = PreviewFunctionSignatureController.class)
@Order(Ordered.HIGHEST_PRECEDENCE)
class PreviewFunctionSignatureErrorAdvice {
  @ExceptionHandler(PreviewBodyTooLargeException.class)
  ProblemDetail tooLarge(PreviewBodyTooLargeException error) {
    ProblemDetail problem =
        ProblemDetail.forStatusAndDetail(HttpStatus.CONTENT_TOO_LARGE, error.getMessage());
    problem.setProperty("errorCode", error.errorCode());
    return problem;
  }
}
