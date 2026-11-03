package com.algoprep.judge.application.ports.out;

import com.algoprep.judge.application.dto.QueueMonitorBatch;

public interface QueueMonitorBroadcastPort {
  void publish(QueueMonitorBatch batch);
}
