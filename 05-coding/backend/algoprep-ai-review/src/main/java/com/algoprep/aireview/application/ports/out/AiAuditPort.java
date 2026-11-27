package com.algoprep.aireview.application.ports.out;

import com.algoprep.aireview.domain.model.AiAuditEntry;

/**
 * Mandatory synchronous audit that joins the caller transaction (REQUIRED,
 * DEC-2026-1102-cross-module-audit-required); never an asynchronous event.
 */
public interface AiAuditPort {
  void record(AiAuditEntry entry);
}
