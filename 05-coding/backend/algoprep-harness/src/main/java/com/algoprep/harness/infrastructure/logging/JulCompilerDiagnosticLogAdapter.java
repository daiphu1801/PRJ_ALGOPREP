package com.algoprep.harness.infrastructure.logging;

import com.algoprep.harness.application.ports.out.CompilerDiagnosticLogPort;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.springframework.stereotype.Component;

/**
 * Real internal log sink; access and retention belong to the deployment's logging configuration.
 */
@Component
final class JulCompilerDiagnosticLogAdapter implements CompilerDiagnosticLogPort {
  private static final Logger LOG =
      Logger.getLogger(JulCompilerDiagnosticLogAdapter.class.getName());

  @Override
  public void write(String correlationId, String boundedOutput, boolean truncated) {
    LOG.log(
        Level.SEVERE,
        "Harness compiler output correlationId={0} truncated={1}\n{2}",
        new Object[] {correlationId, truncated, boundedOutput});
  }
}
