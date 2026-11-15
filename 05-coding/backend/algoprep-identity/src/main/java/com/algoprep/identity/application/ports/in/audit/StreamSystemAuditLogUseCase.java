package com.algoprep.identity.application.ports.in.audit;

import com.algoprep.identity.application.dto.audit.AuditStreamBatch;
import com.algoprep.identity.application.dto.audit.AuditStreamRequest;

/** Reads the rows a live viewer has not seen yet; the transport lives in presentation (#66). */
public interface StreamSystemAuditLogUseCase {
  AuditStreamBatch next(AuditStreamRequest request);
}
