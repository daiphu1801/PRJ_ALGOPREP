package com.algoprep.common.functionspec;

import java.util.List;

/** Hard failures are separate from reasons to use Standard I/O. */
public record SpecEvaluation(
    List<Violation> violations,
    boolean wrapperSupported,
    List<UnsupportedReason> unsupportedReasons) {
  public SpecEvaluation {
    violations = List.copyOf(violations);
    unsupportedReasons = List.copyOf(unsupportedReasons);
  }

  public record Violation(String code, String path, String message) {}

  public record UnsupportedReason(String code, String path) {}
}
