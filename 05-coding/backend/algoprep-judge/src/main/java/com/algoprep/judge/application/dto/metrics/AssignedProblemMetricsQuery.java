package com.algoprep.judge.application.dto.metrics;

import com.algoprep.judge.domain.model.metrics.AssignmentPeriod;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/** Assignments cover every owned class for the requested problems, including other tabs. */
public record AssignedProblemMetricsQuery(
    UUID instructorId,
    Set<UUID> classIds,
    Set<UUID> problemIds,
    List<AssignmentPeriod> assignments) {
  public AssignedProblemMetricsQuery {
    Objects.requireNonNull(instructorId);
    classIds = Set.copyOf(classIds);
    problemIds = Set.copyOf(problemIds);
    assignments = AssignmentPeriod.distinct(assignments);
  }
}
