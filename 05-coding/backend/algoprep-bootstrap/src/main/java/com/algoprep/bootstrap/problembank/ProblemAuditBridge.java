package com.algoprep.bootstrap.problembank;

import com.algoprep.identity.application.ports.in.RecordSystemAuditUseCase;
import com.algoprep.identity.domain.model.AuditEntry;
import com.algoprep.problembank.domain.model.CatalogAuditEntry;
import com.algoprep.problembank.domain.ports.out.ProblemAuditPort;
import org.springframework.stereotype.Component;

/** Translates the caller-owned contract into identity's inbound audit use case. */
@Component
public final class ProblemAuditBridge implements ProblemAuditPort {
  private final RecordSystemAuditUseCase audit;

  public ProblemAuditBridge(RecordSystemAuditUseCase audit) {
    this.audit = audit;
  }

  @Override
  public void record(CatalogAuditEntry entry) {
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
