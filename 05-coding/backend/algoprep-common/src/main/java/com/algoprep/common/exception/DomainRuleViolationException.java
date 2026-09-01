package com.algoprep.common.exception;

/**
 * Một bất biến nghiệp vụ bị vi phạm.
 *
 * <p>Ví dụ trong AlgoPrep: nộp bài bằng một ngôn ngữ ngoài ba ngôn ngữ được hỗ trợ, hoặc ghi đè một
 * trạng thái bài nộp đã là trạng thái cuối ({@code 01-rd/system/backend_architecture.md} mục 4.A
 * điểm 1).
 */
public class DomainRuleViolationException extends AlgoPrepException {

  private static final long serialVersionUID = 1L;

  public DomainRuleViolationException(String errorCode, String message) {
    super(errorCode, message);
  }

  public DomainRuleViolationException(String errorCode, String message, Throwable cause) {
    super(errorCode, message, cause);
  }
}
