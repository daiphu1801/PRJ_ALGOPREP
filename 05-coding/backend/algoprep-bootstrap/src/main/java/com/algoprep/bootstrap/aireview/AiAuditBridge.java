package com.algoprep.bootstrap.aireview;

import com.algoprep.aireview.application.ports.out.AiAuditPort;
import com.algoprep.aireview.domain.model.AiAuditEntry;
import com.algoprep.identity.application.ports.in.RecordSystemAuditUseCase;
import com.algoprep.identity.domain.model.AuditEntry;
import org.springframework.stereotype.Component;

/** DEC-2026-1102-cross-module-audit-required: no independent audit transaction. */
@Component
public final class AiAuditBridge implements AiAuditPort {
  private final RecordSystemAuditUseCase audit;

  public AiAuditBridge(RecordSystemAuditUseCase audit) {
    this.audit = audit;
  }

  @Override
  public void record(AiAuditEntry entry) {
    audit.record(
        new AuditEntry(
            entry.actorUserId(),
            entry.actionType(),
            entry.targetType(),
            entry.targetId(),
            entry.beforeJson(),
            entry.afterJson()));
  }
}
