package com.algoprep.problembank.domain.ports.out;

import com.algoprep.problembank.domain.model.CatalogAuditEntry;

/** Mandatory synchronous audit in the same transaction; never an asynchronous event. */
public interface ProblemAuditPort {
  void record(CatalogAuditEntry entry);
}
