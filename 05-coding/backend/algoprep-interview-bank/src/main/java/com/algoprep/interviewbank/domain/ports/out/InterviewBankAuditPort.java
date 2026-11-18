package com.algoprep.interviewbank.domain.ports.out;

import com.algoprep.interviewbank.domain.model.InterviewAuditEntry;

/**
 * Mandatory synchronous audit that joins the caller transaction (REQUIRED,
 * DEC-2026-1102-cross-module-audit-required); never an asynchronous event.
 */
public interface InterviewBankAuditPort {
  void record(InterviewAuditEntry entry);
}
