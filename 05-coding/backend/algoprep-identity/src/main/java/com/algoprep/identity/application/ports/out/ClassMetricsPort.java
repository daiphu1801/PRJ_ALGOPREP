package com.algoprep.identity.application.ports.out;

import com.algoprep.identity.domain.model.ClassProblemWindow;
import com.algoprep.identity.domain.model.ClassRosterEntry;
import com.algoprep.identity.domain.model.StudentMetrics;
import java.util.List;
import java.util.UUID;

/**
 * What the judge knows about the given students of one class (F4 P1). Answers one entry per
 * student, in the order given, counting only submissions inside the enrolment and assignment
 * windows. A failure throws; it is never turned into empty figures.
 */
public interface ClassMetricsPort {
  List<StudentMetrics> read(
      UUID classId,
      List<ClassRosterEntry> students,
      List<ClassProblemWindow> assignments,
      int weekCount);
}
