package com.algoprep.identity.domain.ports.out;

import com.algoprep.identity.domain.model.ActivityEntry;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** The "recent activity" feed (BD 1.12) and the enrollments needed to scope an entry to a class. */
public interface RecentActivityRepository {

  /** Classes the student was enrolled in at {@code at}: joined by then and not yet left. */
  List<UUID> classesEnrolledAt(UUID studentId, Instant at);

  /** Appends one entry to the feed. */
  void add(ActivityEntry entry);

  /** Appends the entry only when its class still exists; false when the class is gone. */
  boolean addIfClassExists(ActivityEntry entry);
}
