package com.algoprep.identity.application.ports.in.audit;

import com.algoprep.identity.application.dto.audit.AuditExport;
import com.algoprep.identity.application.dto.audit.ExportAuditLogsQuery;

/**
 * Validates an export request, records the export itself in the log, and returns the rows to stream
 * as CSV (#40). Validation errors surface here, before any byte of the body is sent.
 */
public interface ExportSystemAuditLogsUseCase {
  AuditExport open(ExportAuditLogsQuery query);
}
