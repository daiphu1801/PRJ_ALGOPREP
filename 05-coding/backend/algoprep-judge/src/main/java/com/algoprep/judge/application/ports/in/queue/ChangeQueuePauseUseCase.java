package com.algoprep.judge.application.ports.in.queue;

import com.algoprep.judge.application.dto.queue.QueuePauseChange;

/**
 * Trusted transaction kernel, not an admin authorization boundary. A future admin caller must check
 * authority and write its audit in the same surrounding REQUIRED transaction.
 */
public interface ChangeQueuePauseUseCase {
  QueuePauseChange change(boolean expectedPaused, boolean paused);
}
