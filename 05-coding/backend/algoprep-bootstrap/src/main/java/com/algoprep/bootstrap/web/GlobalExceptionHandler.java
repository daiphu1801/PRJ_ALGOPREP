package com.algoprep.bootstrap.web;

import com.algoprep.common.exception.AlgoPrepException;
import com.algoprep.common.exception.ClientErrorDetails;
import com.algoprep.common.exception.DomainRuleViolationException;
import com.algoprep.common.exception.InvalidRequestException;
import com.algoprep.common.exception.PermissionDeniedException;
import com.algoprep.common.exception.RateLimitExceededException;
import com.algoprep.common.exception.ResourceConflictException;
import com.algoprep.common.exception.ResourceNotFoundException;
import com.algoprep.common.exception.ServiceUnavailableException;
import com.algoprep.common.exception.UnauthenticatedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Ánh xạ ngoại lệ nghiệp vụ sang phản hồi HTTP, một chỗ duy nhất cho cả tám module.
 *
 * <p>Trả {@link ProblemDetail} (RFC 9457) kèm thuộc tính {@code errorCode}. Frontend dịch theo
 * {@code errorCode}, KHÔNG hiển thị {@code detail} thô — {@code detail} viết cho log và cho lập
 * trình viên ({@code 01-rd/system/SYS0101_backend_architecture.md} mục 7).
 *
 * <p><strong>Khung base, chưa đầy đủ.</strong> Danh mục mã lỗi và bảng ánh xạ mã lỗi sang HTTP
 * status thuộc {@code 03-dd/api/api.md}, tài liệu chưa tồn tại. Ba nhánh dưới đây bám theo ba loại
 * ngoại lệ nền mà {@code algoprep-common} đã định nghĩa, không phải theo một danh mục mã lỗi.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger LOG = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  @ExceptionHandler(ResourceConflictException.class)
  public ProblemDetail handleConflict(ResourceConflictException ex) {
    return toProblemDetail(ex, HttpStatus.CONFLICT);
  }

  @ExceptionHandler(RateLimitExceededException.class)
  public ProblemDetail handleRateLimit(RateLimitExceededException ex) {
    return toProblemDetail(ex, HttpStatus.TOO_MANY_REQUESTS);
  }

  @ExceptionHandler(ResourceNotFoundException.class)
  public ProblemDetail handleNotFound(ResourceNotFoundException ex) {
    return toProblemDetail(ex, HttpStatus.NOT_FOUND);
  }

  @ExceptionHandler(UnauthenticatedException.class)
  public ProblemDetail handleUnauthenticated(UnauthenticatedException ex) {
    return toProblemDetail(ex, HttpStatus.UNAUTHORIZED);
  }

  @ExceptionHandler(InvalidRequestException.class)
  public ProblemDetail handleInvalidRequest(InvalidRequestException ex) {
    return toProblemDetail(ex, HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(ServiceUnavailableException.class)
  public ProblemDetail handleServiceUnavailable(ServiceUnavailableException ex) {
    LOG.error("Dịch vụ phụ thuộc lỗi [{}]", ex.errorCode(), ex);
    return toProblemDetail(ex, HttpStatus.SERVICE_UNAVAILABLE);
  }

  @ExceptionHandler(PermissionDeniedException.class)
  public ProblemDetail handlePermissionDenied(PermissionDeniedException ex) {
    return toProblemDetail(ex, HttpStatus.FORBIDDEN);
  }

  /**
   * 422 chứ không phải 400: yêu cầu đúng cú pháp và đúng kiểu, nhưng vi phạm một bất biến nghiệp
   * vụ. 400 dành cho yêu cầu mà tầng ngoài không đọc nổi.
   */
  @ExceptionHandler(DomainRuleViolationException.class)
  public ProblemDetail handleDomainRuleViolation(DomainRuleViolationException ex) {
    return toProblemDetail(ex, HttpStatus.UNPROCESSABLE_ENTITY);
  }

  private ProblemDetail toProblemDetail(AlgoPrepException ex, HttpStatusCode status) {
    LOG.warn("Ngoại lệ nghiệp vụ [{}]: {}", ex.errorCode(), ex.getMessage());
    ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, ex.getMessage());
    problem.setProperty("errorCode", ex.errorCode());
    if (ex instanceof ClientErrorDetails metadata && !metadata.clientErrorDetails().isEmpty()) {
      problem.setProperty("details", metadata.clientErrorDetails());
    }
    return problem;
  }
}
