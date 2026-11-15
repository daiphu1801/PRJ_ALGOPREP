package com.algoprep.identity.application.ports.in.audit;

import com.algoprep.identity.application.dto.audit.AuditLogPageResult;
import com.algoprep.identity.application.dto.audit.SearchAuditLogsQuery;

/** Searches and filters the system audit log, 50 rows per cursor page (#39). */
public interface SearchSystemAuditLogsUseCase {
  AuditLogPageResult search(SearchAuditLogsQuery query);
}
