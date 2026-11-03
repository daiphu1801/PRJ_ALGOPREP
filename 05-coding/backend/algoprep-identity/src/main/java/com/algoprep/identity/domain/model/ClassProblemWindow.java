package com.algoprep.identity.domain.model;

import java.time.Instant;
import java.util.UUID;

/**
 * One interval in which a class had a problem assigned: {@code [assignedAt, removedAt)}, with a
 * null end for an assignment that is still current. Removed and repeated assignments both appear.
 */
public record ClassProblemWindow(
    UUID classId, UUID problemId, Instant assignedAt, Instant removedAt) {

  /** True when {@code at} falls inside the interval. */
  public boolean covers(Instant at) {
    return !at.isBefore(assignedAt) && (removedAt == null || at.isBefore(removedAt));
  }
}
