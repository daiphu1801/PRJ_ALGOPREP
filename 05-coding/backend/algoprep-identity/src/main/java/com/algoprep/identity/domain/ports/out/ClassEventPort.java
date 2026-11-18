package com.algoprep.identity.domain.ports.out;

import java.time.Instant;
import java.util.UUID;

/**
 * Domain events of a class leaving other modules' data behind (DD api F1 section 2). Delivery is
 * after commit and best effort: a failure is logged, never thrown (BR-11, BR-22).
 */
public interface ClassEventPort {
  void classDeleted(UUID classId, Instant occurredAt);

  void studentRemoved(UUID classId, UUID studentId, Instant occurredAt);
}
