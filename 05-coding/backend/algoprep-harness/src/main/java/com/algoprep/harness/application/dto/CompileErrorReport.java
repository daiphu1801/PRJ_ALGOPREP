package com.algoprep.harness.application.dto;

import com.algoprep.harness.domain.model.CompileDiagnostic;
import com.algoprep.harness.domain.model.CompileReportStatus;
import java.util.List;

/** Learner-facing P3 result; the caller already has signatureText for the fixed message. */
public record CompileErrorReport(
    CompileReportStatus status,
    List<CompileDiagnostic> diagnostics,
    String fixedMessageKey,
    String correlationId) {
  public CompileErrorReport {
    diagnostics = List.copyOf(diagnostics);
  }
}
