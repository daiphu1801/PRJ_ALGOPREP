package com.algoprep.problembank.application.ports.out;

import com.algoprep.problembank.domain.model.ClassAssignmentModels.Metric;
import com.algoprep.problembank.domain.model.ClassAssignmentSnapshot;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** Submission counts and Accepted rate per assigned problem, from the judge module (F4 P6). */
public interface ClassProblemMetricsPort {
  /**
   * {@code history} is every assignment interval of the instructor's classes for these problems, so
   * a submission is attributed to one class before the class filter applies. Empty when the judge
   * module cannot answer; callers then show no numbers rather than zeros.
   */
  Optional<Map<UUID, Metric>> read(
      UUID instructorId,
      Set<UUID> classIds,
      Set<UUID> problemIds,
      List<ClassAssignmentSnapshot> history);
}
