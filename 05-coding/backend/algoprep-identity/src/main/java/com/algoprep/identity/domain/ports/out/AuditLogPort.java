package com.algoprep.identity.domain.ports.out;

import com.algoprep.identity.domain.model.AuditEntry;

/**
 * Appends one row to {@code identity.system_audit_logs} (DD BR-24).
 *
 * <p>Runs in the caller's transaction: if the business change rolls back, the audit row goes with
 * it, and a failure to write the row fails the whole transaction.
 *
 * <p>BR-24 lists six positional arguments; Checkstyle caps parameters at five, so they travel as
 * one {@link AuditEntry} with the same six fields.
 */
public interface AuditLogPort {

  /**
   * @param entry the row to append
   */
  void record(AuditEntry entry);
}
