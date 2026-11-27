package com.algoprep.harness.application.ports.out;

/** Internal ERROR sink; never expose its bounded raw payload through a learner-facing API. */
public interface CompilerDiagnosticLogPort {
  /** Logging failure is isolated by the handler and must not leak raw data into exceptions. */
  void write(String correlationId, String boundedOutput, boolean truncated);
}
