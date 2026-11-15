package com.algoprep.judge.application.command;

import com.algoprep.judge.application.ports.in.BroadcastQueueMonitorUseCase;
import com.algoprep.judge.application.ports.in.GetQueueMonitorBatchUseCase;
import com.algoprep.judge.application.ports.out.QueueMonitorAudiencePort;
import com.algoprep.judge.application.ports.out.QueueMonitorBroadcastPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
class BroadcastQueueMonitorCommandHandler implements BroadcastQueueMonitorUseCase {
  private static final Logger LOG =
      LoggerFactory.getLogger(BroadcastQueueMonitorCommandHandler.class);
  private final QueueMonitorAudiencePort audience;
  private final GetQueueMonitorBatchUseCase batches;
  private final QueueMonitorBroadcastPort broadcast;

  BroadcastQueueMonitorCommandHandler(
      QueueMonitorAudiencePort audience,
      GetQueueMonitorBatchUseCase batches,
      QueueMonitorBroadcastPort broadcast) {
    this.audience = audience;
    this.batches = batches;
    this.broadcast = broadcast;
  }

  @Override
  public void broadcast() {
    try {
      if (audience.hasSubscribers()) {
        var batch = batches.read();
        if (audience.hasSubscribers()) {
          broadcast.publish(batch);
        }
      }
    } catch (RuntimeException failure) {
      // No false empty batch and no cause, SQL, payload or token in logs; REST remains available.
      LOG.warn("Queue monitor batch delivery failed");
    }
  }
}
