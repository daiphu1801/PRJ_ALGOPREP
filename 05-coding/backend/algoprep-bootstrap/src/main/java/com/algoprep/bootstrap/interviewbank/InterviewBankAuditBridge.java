package com.algoprep.bootstrap.interviewbank;

import com.algoprep.identity.application.ports.in.RecordSystemAuditUseCase;
import com.algoprep.identity.domain.model.AuditEntry;
import com.algoprep.interviewbank.domain.model.InterviewAuditEntry;
import com.algoprep.interviewbank.domain.ports.out.InterviewBankAuditPort;
import org.springframework.stereotype.Component;

/** DEC-2026-1102-cross-module-audit-required: no independent audit transaction. */
@Component
public final class InterviewBankAuditBridge implements InterviewBankAuditPort {
  private final RecordSystemAuditUseCase audit;

  public InterviewBankAuditBridge(RecordSystemAuditUseCase audit) {
    this.audit = audit;
  }

  @Override
  public void record(InterviewAuditEntry entry) {
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
