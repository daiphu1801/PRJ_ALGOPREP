package com.algoprep.judge.application.ports.in;

import com.algoprep.judge.domain.model.QueueMonitoring.Snapshot;

/** API #13: current queue state from persisted timestamps and configuration. */
public interface GetQueueSnapshotUseCase {
  Snapshot read();
}
