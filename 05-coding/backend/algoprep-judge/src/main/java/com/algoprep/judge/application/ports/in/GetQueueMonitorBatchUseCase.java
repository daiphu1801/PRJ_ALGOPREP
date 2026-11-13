package com.algoprep.judge.application.ports.in;

import com.algoprep.judge.application.dto.QueueMonitorBatch;

public interface GetQueueMonitorBatchUseCase {
  QueueMonitorBatch read();
}
