package com.algoprep.identity.application.ports.in;

import com.algoprep.identity.application.dto.AuditLogPageResult;
import com.algoprep.identity.application.dto.SearchAuditLogsQuery;

/** Searches and filters the system audit log, 50 rows per cursor page (#39). */
public interface SearchSystemAuditLogsUseCase {
  AuditLogPageResult search(SearchAuditLogsQuery query);
}
