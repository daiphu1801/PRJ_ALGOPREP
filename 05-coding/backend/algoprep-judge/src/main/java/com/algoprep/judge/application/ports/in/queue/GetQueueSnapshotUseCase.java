package com.algoprep.judge.application.ports.in.queue;

import com.algoprep.judge.domain.model.queue.QueueMonitoring.Snapshot;

/** API #13: current queue state from persisted timestamps and configuration. */
public interface GetQueueSnapshotUseCase {
  Snapshot read();
}
