package com.algoprep.interviewbank.domain.model;

/** Self rating and its fixed review interval (DD logic F6 BR-IB-06). */
public enum RecallRating {
  FORGOTTEN(1),
  VAGUE(3),
  KNOWN(7);

  private final int intervalDays;

  RecallRating(int intervalDays) {
    this.intervalDays = intervalDays;
  }

  public int intervalDays() {
    return intervalDays;
  }
}
