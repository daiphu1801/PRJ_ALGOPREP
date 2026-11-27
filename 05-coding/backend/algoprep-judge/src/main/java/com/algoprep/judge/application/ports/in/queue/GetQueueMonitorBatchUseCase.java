package com.algoprep.judge.application.ports.in.queue;

import com.algoprep.judge.application.dto.queue.QueueMonitorBatch;

public interface GetQueueMonitorBatchUseCase {
  QueueMonitorBatch read();
}
