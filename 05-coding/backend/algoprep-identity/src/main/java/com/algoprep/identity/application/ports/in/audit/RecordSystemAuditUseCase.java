package com.algoprep.identity.application.ports.in.audit;

import com.algoprep.identity.domain.model.audit.AuditEntry;

/** Synchronous internal audit boundary; joins the caller transaction. */
public interface RecordSystemAuditUseCase {
  void record(AuditEntry entry);
}
