package com.algoprep.interviewbank.domain.model;

/** Final state of a stored attempt; PENDING is never written (DD api F6 section 28, D8). */
public enum FeedbackStatus {
  COMPLETED,
  FAILED
}
