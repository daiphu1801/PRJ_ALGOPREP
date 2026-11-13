package com.algoprep.judge.application.ports.out;

import java.util.UUID;

/**
 * Sends only the persisted submission id. True requires broker confirmation and no mandatory
 * return; at most three bounded attempts with 200/500 ms waits. False includes unknown delivery, so
 * a worker may already have received it. Failure resolution must use the guarded PENDING update.
 */
public interface SubmissionQueuePort {
  boolean enqueue(UUID submissionId);
}
