package com.algoprep.judge.application.dto;

import com.algoprep.judge.domain.model.QueueMonitoring.Counts;
import com.algoprep.judge.domain.model.QueueMonitoring.Snapshot;
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
