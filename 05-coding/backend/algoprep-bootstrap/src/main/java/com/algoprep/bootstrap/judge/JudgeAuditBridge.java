package com.algoprep.bootstrap.judge;

import com.algoprep.identity.application.ports.in.RecordSystemAuditUseCase;
import com.algoprep.identity.domain.model.AuditEntry;
import com.algoprep.judge.domain.model.JudgeAuditEntry;
import com.algoprep.judge.domain.ports.out.JudgeAuditPort;
import org.springframework.stereotype.Component;

/** DEC-2026-1102-cross-module-audit-required: no independent audit transaction. */
@Component
public final class JudgeAuditBridge implements JudgeAuditPort {
  private final RecordSystemAuditUseCase audit;

  public JudgeAuditBridge(RecordSystemAuditUseCase audit) {
    this.audit = audit;
  }

  @Override
  public void record(JudgeAuditEntry entry) {
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
