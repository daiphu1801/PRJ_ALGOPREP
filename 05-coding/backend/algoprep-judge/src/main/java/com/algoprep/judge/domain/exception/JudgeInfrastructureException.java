package com.algoprep.judge.domain.exception;

/** Fixed text only: transport exceptions may contain payload or cached identifiers. */
public final class JudgeInfrastructureException extends RuntimeException {
  private static final long serialVersionUID = 1L;

  public JudgeInfrastructureException() {
    super("Judge sandbox is unavailable");
  }
}
