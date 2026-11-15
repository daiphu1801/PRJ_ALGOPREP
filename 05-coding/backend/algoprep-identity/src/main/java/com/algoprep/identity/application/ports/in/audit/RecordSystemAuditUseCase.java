package com.algoprep.identity.application.ports.in;

import com.algoprep.identity.domain.model.AuditEntry;

/** Synchronous internal audit boundary; joins the caller transaction. */
public interface RecordSystemAuditUseCase {
  void record(AuditEntry entry);
}
