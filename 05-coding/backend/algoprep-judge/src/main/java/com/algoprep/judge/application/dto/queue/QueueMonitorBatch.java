package com.algoprep.judge.application.dto.queue;

import com.algoprep.judge.domain.model.queue.QueueMonitoring.Counts;
import com.algoprep.judge.domain.model.queue.QueueMonitoring.Snapshot;
import java.time.Instant;

/** DD W2: aggregate-only refresh hint, never the tab-dependent job list. */
public record QueueMonitorBatch(
    String type,
    Instant generatedAt,
    Snapshot snapshot,
    Counts jobCounts,
    InfraSummary infraSummary) {
  public record InfraSummary(long warnCount, long errorCount) {}
}
