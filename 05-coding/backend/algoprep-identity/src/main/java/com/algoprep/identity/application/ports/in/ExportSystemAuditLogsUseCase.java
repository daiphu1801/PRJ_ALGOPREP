package com.algoprep.identity.application.ports.in;

import com.algoprep.identity.application.dto.AuditExport;
import com.algoprep.identity.application.dto.ExportAuditLogsQuery;

/**
 * Validates an export request, records the export itself in the log, and returns the rows to stream
 * as CSV (#40). Validation errors surface here, before any byte of the body is sent.
 */
public interface ExportSystemAuditLogsUseCase {
  AuditExport open(ExportAuditLogsQuery query);
}
