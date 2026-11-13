package com.algoprep.identity.domain.model;

import java.time.LocalDate;
import java.util.List;

/** The 364 consecutive UTC+7 days of #55, oldest first, zero-filled. */
public record ActivityCalendar(LocalDate from, LocalDate to, List<Day> days) {

  /** Submissions counted on one day. */
  public record Day(LocalDate day, int submissionCount) {}
}
