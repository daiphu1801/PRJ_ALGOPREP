package com.algoprep.judge.domain.ports.out;

import com.algoprep.judge.domain.model.JudgeAuditEntry;

/** REQUIRED audit: failure rolls back the administrative write. */
public interface JudgeAuditPort {
  void record(JudgeAuditEntry entry);
}
