package com.algoprep.interviewbank.presentation.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Method security throws before the controller runs, so this advice shapes INB-301 and INB-V*. */
@RestControllerAdvice(
    assignableTypes = {
      QuestionTopicsController.class,
      QuestionLevelsController.class,
      AdminQuestionBulkController.class,
      AdminQuestionsController.class,
      InterviewQuestionsController.class,
      InterviewTopicProgressController.class,
      PracticeAnswersController.class
    })
@Order(Ordered.HIGHEST_PRECEDENCE)
class InterviewBankErrorAdvice {
  @ExceptionHandler(AccessDeniedException.class)
  ProblemDetail denied() {
    return error(HttpStatus.FORBIDDEN, "INB-301", "Interview bank management permission required");
  }

  @ExceptionHandler(BodyTooLargeException.class)
  ProblemDetail tooLarge(BodyTooLargeException failure) {
    return error(HttpStatus.CONTENT_TOO_LARGE, failure.errorCode(), failure.getMessage());
  }

  /** An unreadable body is a shape error of the endpoint: order list or display name. */
  @ExceptionHandler(HttpMessageNotReadableException.class)
  ProblemDetail unreadable(HttpServletRequest request) {
    String path = request.getRequestURI();
    String code = path.endsWith("/order") ? "INB-V14" : "INB-V03";
    if (path.endsWith("/recall")) {
      code = "INB-V04";
    } else if (path.endsWith("/bookmark")) {
      code = "INB-V05";
    } else if (path.endsWith("/practice-answers")) {
      code = "INB-V06";
    }
    return error(
        HttpStatus.BAD_REQUEST, code, "Request body is not readable JSON of the expected shape");
  }

  private static ProblemDetail error(HttpStatus status, String code, String message) {
    var detail = ProblemDetail.forStatusAndDetail(status, message);
    detail.setProperty("errorCode", code);
    return detail;
  }
}
