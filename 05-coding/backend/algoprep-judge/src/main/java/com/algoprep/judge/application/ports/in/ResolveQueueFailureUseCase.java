package com.algoprep.judge.application.ports.in;

import com.algoprep.judge.application.dto.QueueFailureResolution;
import java.util.UUID;

/** Trusted finalization kernel. Commits before return; no public route or realtime transport. */
public interface ResolveQueueFailureUseCase {
  QueueFailureResolution resolve(UUID submissionId);
}
