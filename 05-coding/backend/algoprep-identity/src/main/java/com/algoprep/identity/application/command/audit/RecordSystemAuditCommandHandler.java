package com.algoprep.identity.application.command;

import com.algoprep.identity.application.ports.in.RecordSystemAuditUseCase;
import com.algoprep.identity.domain.model.AuditEntry;
import com.algoprep.identity.domain.ports.out.AuditLogPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** D1: REQUIRED deliberately shares rollback with the calling context. */
@Service
class RecordSystemAuditCommandHandler implements RecordSystemAuditUseCase {
  private final AuditLogPort audit;

  RecordSystemAuditCommandHandler(AuditLogPort audit) {
    this.audit = audit;
  }

  @Override
  @Transactional
  public void record(AuditEntry entry) {
    audit.record(entry);
  }
}
