package com.algoprep.identity.application.dto;

/** Outcome of issuing an e-mail change code. */
public enum EmailChangeIssueResult {
  ISSUED,
  /** Another code was issued less than 60 seconds ago. */
  COOLDOWN,
  /** Three requests already used in the last 24 hours (resends count). */
  BUDGET_EXCEEDED
}
