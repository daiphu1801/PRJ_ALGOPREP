package com.algoprep.aireview.domain.model;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

/** Pure budget rules of BR-AI-05; the budget is a soft cap, not a hard cap. */
public final class BudgetPolicy {
  /** The budget period starts at 00:00 UTC+7 of {@code period_start}. */
  static final ZoneId BUDGET_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

  private BudgetPolicy() {}

  public static Instant periodStartAt(LocalDate periodStart) {
    return periodStart.atStartOfDay(BUDGET_ZONE).toInstant();
  }

  /** Used up: a configured limit that the used total has reached. 0 means "not configured". */
  public static boolean exhausted(long limitTokens, long usedTokens) {
    return limitTokens > 0 && usedTokens >= limitTokens;
  }

  /** Calendar day in UTC+7. */
  public static LocalDate dayOf(Instant instant) {
    return instant.atZone(BUDGET_ZONE).toLocalDate();
  }

  /** First day of the UTC+7 month that contains the instant (JOB-AI-03 target period). */
  public static LocalDate monthStartOf(Instant instant) {
    return dayOf(instant).withDayOfMonth(1);
  }

  /**
   * Linear forecast of BR-AI-05 step 6: {@code today + ceil(remaining / (windowTokens / days))}.
   * Null when the window has no usage; the caller skips it when locked or unconfigured.
   */
  public static LocalDate projectedRunOutDate(
      long remaining, long windowTokens, long windowDays, LocalDate today) {
    if (windowTokens <= 0) {
      return null;
    }
    long daysLeft = Math.ceilDiv(remaining * Math.max(1, windowDays), windowTokens);
    return today.plusDays(daysLeft);
  }

  /** Locked by an earlier lock, or exhausted right now (the gate heals itself). */
  public static boolean locked(BudgetConfig config, long usedTokens) {
    return config.lockedAt() != null || exhausted(config.limitTokens(), usedTokens);
  }
}
