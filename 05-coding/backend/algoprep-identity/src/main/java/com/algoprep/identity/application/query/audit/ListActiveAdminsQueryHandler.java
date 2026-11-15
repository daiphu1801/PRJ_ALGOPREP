package com.algoprep.identity.application.query;

import com.algoprep.identity.application.ports.in.ListActiveAdminsUseCase;
import com.algoprep.identity.domain.model.ActiveAdmin;
import com.algoprep.identity.domain.ports.out.AuditLogReadRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** #64: at most five admins with actions in the last 24 hours; empty when there are none. */
@Service
class ListActiveAdminsQueryHandler implements ListActiveAdminsUseCase {
  private final AuditLogReadRepository logs;

  ListActiveAdminsQueryHandler(AuditLogReadRepository logs) {
    this.logs = logs;
  }

  @Override
  @Transactional(readOnly = true)
  public List<ActiveAdmin> list() {
    return logs.activeAdmins();
  }
}
