package com.algoprep.interviewbank.domain.model;

/** Learner list status tab; REVIEWING is VAGUE or FORGOTTEN, regardless of the due date. */
public enum RecallFilter {
  ALL,
  UNSEEN,
  REVIEWING,
  KNOWN
}
