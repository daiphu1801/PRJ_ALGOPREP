package com.algoprep.judge.domain.exception;

public final class JudgeReadUnavailableException extends RuntimeException {
  public JudgeReadUnavailableException() {
    super("Judge read unavailable");
  }
}
