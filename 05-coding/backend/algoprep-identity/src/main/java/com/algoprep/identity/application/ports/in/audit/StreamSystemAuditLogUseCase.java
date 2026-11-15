package com.algoprep.identity.application.ports.in;

import com.algoprep.identity.application.dto.AuditStreamBatch;
import com.algoprep.identity.application.dto.AuditStreamRequest;

/** Reads the rows a live viewer has not seen yet; the transport lives in presentation (#66). */
public interface StreamSystemAuditLogUseCase {
  AuditStreamBatch next(AuditStreamRequest request);
}
