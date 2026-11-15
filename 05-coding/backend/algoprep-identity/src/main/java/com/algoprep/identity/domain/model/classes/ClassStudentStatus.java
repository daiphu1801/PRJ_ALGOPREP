package com.algoprep.identity.domain.model;

/**
 * The five labels of BR-10, in the order they are tested: the first label whose condition holds
 * wins.
 */
public enum ClassStudentStatus {
  INSUFFICIENT_DATA,
  ABSENT,
  NEEDS_SUPPORT,
  WATCH,
  ON_TRACK
}
