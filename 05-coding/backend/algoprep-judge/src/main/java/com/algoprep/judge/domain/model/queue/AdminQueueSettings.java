package com.algoprep.judge.domain.model;

import java.time.Instant;
import java.util.Map;

/** Persisted queue settings for admin API #18/#19; timestamp is response-only. */
public record AdminQueueSettings(
    int autoscaleJobThreshold,
    int jobsPerPage,
    int errorRetentionHours,
    int latencyCapSeconds,
    Instant updatedAt) {
  public Map<String, Integer> values() {
    return Map.of(
        "autoscaleJobThreshold",
        autoscaleJobThreshold,
        "jobsPerPage",
        jobsPerPage,
        "errorRetentionHours",
        errorRetentionHours,
        "latencyCapSeconds",
        latencyCapSeconds);
  }
}
