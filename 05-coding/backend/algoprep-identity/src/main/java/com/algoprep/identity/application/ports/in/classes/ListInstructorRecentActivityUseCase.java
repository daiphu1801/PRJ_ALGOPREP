package com.algoprep.identity.application.ports.in;

import com.algoprep.identity.domain.model.ClassActivityItem;
import java.util.List;
import java.util.UUID;

/** Class activity feed #45 (10 entries, 90 days); {@code classId} null means every owned class. */
public interface ListInstructorRecentActivityUseCase {
  List<ClassActivityItem> list(UUID instructorId, UUID classId);
}
