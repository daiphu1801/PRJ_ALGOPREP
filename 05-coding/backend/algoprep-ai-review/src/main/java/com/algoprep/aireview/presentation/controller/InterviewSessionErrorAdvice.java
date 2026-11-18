package com.algoprep.aireview.presentation.controller;

import com.algoprep.aireview.domain.exception.AiInvalidResponseException;
import com.algoprep.aireview.domain.exception.AiRateLimitedException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * The two AIR responses the shared handler cannot express, for the interview endpoints: 429 with
 * {@code Retry-After} (AIR-403, AIR-404) and 502 for a malformed opening or hint (AIR-502). Every
 * other code maps through its base exception (AIR-110 404, AIR-302 403, AIR-111, 113, 114 409,
 * AIR-115, 117 422, AIR-401 429, AIR-501 503, AIR-V* 400).
 */
@RestControllerAdvice(
    assignableTypes = {
      InterviewSessionController.class,
      InterviewTurnController.class,
      InterviewSummaryController.class
    })
@Order(Ordered.HIGHEST_PRECEDENCE)
class InterviewSessionErrorAdvice {
  @ExceptionHandler(AiRateLimitedException.class)
  ResponseEntity<ProblemDetail> rateLimited(AiRateLimitedException failure) {
    ProblemDetail problem =
        ProblemDetail.forStatusAndDetail(HttpStatus.TOO_MANY_REQUESTS, failure.getMessage());
    problem.setProperty("errorCode", failure.errorCode());
    problem.setProperty("details", failure.clientErrorDetails());
    return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
        .header(HttpHeaders.RETRY_AFTER, Long.toString(failure.retryAfterSeconds()))
        .body(problem);
  }

  @ExceptionHandler(AiInvalidResponseException.class)
  ProblemDetail invalidResponse(AiInvalidResponseException failure) {
    ProblemDetail problem =
        ProblemDetail.forStatusAndDetail(HttpStatus.BAD_GATEWAY, failure.getMessage());
    problem.setProperty("errorCode", failure.errorCode());
    return problem;
  }
}
