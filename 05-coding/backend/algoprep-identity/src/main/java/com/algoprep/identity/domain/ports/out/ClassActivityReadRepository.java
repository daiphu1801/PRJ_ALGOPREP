package com.algoprep.identity.domain.ports.out;

import com.algoprep.identity.domain.model.ClassActivityItem;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Feed of the classes one instructor runs (#45). */
public interface ClassActivityReadRepository {

  /**
   * Newest first (ties by id), only entries scoped to a class owned by {@code instructorId}, at or
   * after {@code since}; {@code classId} null means every owned class.
   */
  List<ClassActivityItem> recent(UUID instructorId, UUID classId, Instant since, int limit);
}
