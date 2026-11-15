package com.algoprep.judge.application.ports.in;

import java.util.UUID;

/** Returns only after a terminal commit, or an explicitly ignorable missing/busy/terminal job. */
public interface ProcessQueuedSubmissionUseCase {
  void process(UUID submissionId);
}
