package com.algoprep.identity.domain.model;

import java.util.List;

/**
 * One card of the admin overview: the number, how it moved against the period before, and seven
 * points for the little line. {@code deltaPercent} is null and the direction {@code "none"} when
 * the period before was zero (ADM0101 sheet 7.1: never divide by zero).
 */
public record StatSummary(
    long value, Double deltaPercent, String deltaDirection, List<Long> sparklineSeries) {

  /** Compares {@code current} with {@code previous}; the percent is rounded to one decimal. */
  public static StatSummary of(long current, long previous, List<Long> sparkline) {
    if (previous == 0) {
      return new StatSummary(current, null, "none", sparkline);
    }
    double percent = Math.round((current - previous) * 1000.0 / previous) / 10.0;
    return new StatSummary(current, percent, current >= previous ? "up" : "down", sparkline);
  }
}
