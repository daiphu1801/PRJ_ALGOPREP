package com.algoprep.judge.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** The four configuration groups returned by admin API #21. */
public record AdminLanguageConfig(
    List<Language> languages,
    Defaults gradingDefaults,
    Sandbox sandbox,
    Generator generatorSandbox) {
  public AdminLanguageConfig {
    languages = List.copyOf(languages);
  }

  public record Language(
      String language,
      String compilerDisplayName,
      BigDecimal timeLimitMultiplier,
      BigDecimal memoryLimitMultiplier,
      boolean enabled,
      long pendingJobCount,
      Instant updatedAt) {}

  public record Defaults(
      int timeLimitMs,
      int memoryLimitMb,
      int maxOutputSizeKb,
      int compileTimeoutSeconds,
      Instant updatedAt) {}

  public record Sandbox(
      boolean networkAccessEnabled,
      boolean childProcessLimitEnabled,
      boolean returnStderrToStudent,
      Instant updatedAt) {}

  public record Generator(int maxRuntimeSeconds, int maxOutputMb, Instant updatedAt) {}
}
