package com.algoprep.judge.application.dto;

import com.algoprep.judge.domain.exception.AdminConfigException;
import java.util.Map;

/** Positive PostgreSQL ints, without an additional business ceiling (DD #19). */
public record QueueSettingsChange(
    int autoscaleJobThreshold, int jobsPerPage, int errorRetentionHours, int latencyCapSeconds) {
  public QueueSettingsChange {
    if (autoscaleJobThreshold < 1
        || jobsPerPage < 1
        || errorRetentionHours < 1
        || latencyCapSeconds < 1) {
      throw new AdminConfigException(400, "JDG-V09");
    }
  }

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
