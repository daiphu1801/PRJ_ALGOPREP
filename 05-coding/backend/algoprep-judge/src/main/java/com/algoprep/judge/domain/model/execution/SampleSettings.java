package com.algoprep.judge.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

public record SampleSettings(String language, boolean enabled, WorkerSettings execution) {
  public record Effective(
      String language,
      boolean enabled,
      long timeLimitMs,
      long memoryLimitMb,
      int maxOutputSizeKb) {}

  public Effective effective(int timeMs, int memoryMb, int outputKb) {
    return new Effective(
        language,
        enabled,
        scaled(timeMs, execution.timeMultiplier()),
        scaled(memoryMb, execution.memoryMultiplier()),
        outputKb);
  }

  private static long scaled(int base, BigDecimal multiplier) {
    return BigDecimal.valueOf(base)
        .multiply(multiplier)
        .setScale(0, RoundingMode.HALF_UP)
        .longValueExact();
  }
}
