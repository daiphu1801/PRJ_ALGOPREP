package com.algoprep.judge.application.query;

import com.algoprep.judge.application.dto.QueueMonitorBatch;
import com.algoprep.judge.application.dto.QueueMonitorBatch.InfraSummary;
import com.algoprep.judge.application.ports.in.GetInfraSummaryUseCase;
import com.algoprep.judge.application.ports.in.GetQueueMonitorBatchUseCase;
import com.algoprep.judge.application.ports.in.GetQueueSnapshotUseCase;
import com.algoprep.judge.domain.ports.out.QueueMonitorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
class GetQueueMonitorBatchQueryHandler implements GetQueueMonitorBatchUseCase {
  private final GetQueueSnapshotUseCase snapshot;
  private final QueueMonitorRepository queue;
  private final GetInfraSummaryUseCase infra;

  GetQueueMonitorBatchQueryHandler(
      GetQueueSnapshotUseCase snapshot,
      QueueMonitorRepository queue,
      GetInfraSummaryUseCase infra) {
    this.snapshot = snapshot;
    this.queue = queue;
    this.infra = infra;
  }

  @Override
  @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
  public QueueMonitorBatch read() {
    var state = snapshot.read();
    var counts = queue.counts(queue.jobSettings());
    var summary = infra.summary();
    return new QueueMonitorBatch(
        "QUEUE_BATCH",
        state.generatedAt(),
        state,
        counts,
        new InfraSummary(summary.warnCount(), summary.errorCount()));
  }
}
