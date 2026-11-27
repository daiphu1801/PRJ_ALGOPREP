package com.algoprep.judge.domain.model.execution;

/**
 * What the elected grading worker publishes after each scale poll, so the admin queue snapshot can
 * show the consumers even when the worker runs in another process than the API.
 *
 * @param target consumer target after this poll
 * @param active consumers the listener container reports as running
 * @param scale the target change made by this poll; null when the poll changed nothing
 */
public record WorkerReport(int target, int active, ScaleEvent scale) {
  /** {@code reason} is BACKLOG, IDLE or DISABLED. */
  public record ScaleEvent(int from, int to, String reason, long pendingCount) {}
}
