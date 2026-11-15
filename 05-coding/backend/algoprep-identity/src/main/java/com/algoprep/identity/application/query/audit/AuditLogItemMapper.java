package com.algoprep.identity.application.query.audit;

import com.algoprep.identity.application.dto.audit.AuditLogItem;
import com.algoprep.identity.domain.model.audit.AuditActionCatalog;
import com.algoprep.identity.domain.model.audit.AuditActionInfo;
import com.algoprep.identity.domain.model.audit.AuditLogCursor;
import com.algoprep.identity.domain.model.audit.AuditLogRow;

/**
 * The single mapping layer from a stored row to what leaves the server, shared by search, export
 * and the live stream, so BR-16 masking is applied in exactly one place (DD BR-16).
 */
final class AuditLogItemMapper {

  private AuditLogItemMapper() {}

  static AuditLogItem toItem(AuditLogRow row) {
    AuditActionInfo info = AuditActionCatalog.resolve(row.actionType());
    return new AuditLogItem(
        row.id(),
        row.createdAt(),
        row.actorDisplayName(),
        row.actorEmail(),
        row.actionType(),
        info.category().name(),
        info.service(),
        row.targetType(),
        row.targetId(),
        AuditLogMasker.safeParams(row.beforeJson(), row.afterJson()));
  }

  static AuditLogCursor cursorOf(AuditLogRow row) {
    return new AuditLogCursor(row.createdAt(), row.id());
  }
}
