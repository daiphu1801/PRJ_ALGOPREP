package com.algoprep.common.exception;

/**
 * The request is malformed at the transport level, for example a required header is missing. Maps
 * to HTTP 400; rule violations on well-formed input are {@link DomainRuleViolationException}.
 */
public class InvalidRequestException extends AlgoPrepException {

  private static final long serialVersionUID = 1L;

  public InvalidRequestException(String errorCode, String message) {
    super(errorCode, message);
  }
}
