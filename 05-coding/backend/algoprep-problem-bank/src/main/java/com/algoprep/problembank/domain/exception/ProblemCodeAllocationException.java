package com.algoprep.problembank.domain.exception;

import com.algoprep.common.exception.ResourceConflictException;

/** Even the UUID-backed candidate conflicted; retry creation (HTTP 409 PRB-059). */
public final class ProblemCodeAllocationException extends ResourceConflictException {
  private static final long serialVersionUID = 1L;

  public ProblemCodeAllocationException() {
    super("PRB-059", "Problem code could not be allocated; retry creation");
  }
}
