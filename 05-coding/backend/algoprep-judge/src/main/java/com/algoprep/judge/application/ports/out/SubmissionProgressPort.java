package com.algoprep.judge.application.ports.out;

import com.algoprep.judge.application.dto.SubmissionProgress;
import java.util.UUID;

/** Best-effort F4 realtime transport, separate from durable domain-event Outbox. */
public interface SubmissionProgressPort {
  /**
   * Send after the current transaction commits, or immediately if called after commit without a
   * transaction. The trusted caller supplies the owner read from submissions, never a client id.
   * Rollbacks send nothing; delivery failures only log a fixed WARN and never fail grading.
   */
  void afterCommit(UUID ownerId, SubmissionProgress progress);
}
