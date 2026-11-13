package com.algoprep.judge.domain.model;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Minimal queue projections: no learner source, testcase, problem or user metadata. */
public final class QueueMonitoring {
  private QueueMonitoring() {}

  public record Facts(
      Instant generatedAt,
      boolean paused,
      boolean autoscaleEnabled,
      int autoscaleJobThreshold,
      long pendingCount,
      Long avgWaitMillis,
      int latencyCapSeconds) {}

  public record Snapshot(
      Instant generatedAt,
      String status,
      long pendingCount,
      boolean queueConsumptionPaused,
      boolean autoscaleEnabled,
      int autoscaleJobThreshold,
      Long avgWaitMillis,
      int latencyCapSeconds,
      int workerAutoscaleMax) {
    public static Snapshot from(Facts facts, int maximum) {
      String status =
          facts.paused()
              ? "PAUSED"
              : facts.pendingCount() > facts.autoscaleJobThreshold() ? "BACKLOGGED" : "NORMAL";
      return new Snapshot(
          facts.generatedAt(),
          status,
          facts.pendingCount(),
          facts.paused(),
          facts.autoscaleEnabled(),
          facts.autoscaleJobThreshold(),
          facts.avgWaitMillis(),
          facts.latencyCapSeconds(),
          maximum);
    }
  }

  public record JobSettings(int pageSize, int errorRetentionHours, Instant asOf) {}

  public record Counts(long waiting, long failed, long all) {
    public long forTab(String tab) {
      return switch (tab) {
        case "all" -> all;
        case "failed" -> failed;
        default -> waiting;
      };
    }
  }

  public record Job(UUID submissionId, String language, String status, Long waitingMillis) {}

  public record Jobs(List<Job> items, int page, int pageSize, long totalCount, Counts counts) {
    public Jobs {
      items = List.copyOf(items);
    }
  }
}
