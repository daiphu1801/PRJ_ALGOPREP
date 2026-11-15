package com.algoprep.judge.domain.model;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/** All authorized candidate windows, with the tab filter applied only after attribution. */
public record AssignmentMetricScope(
    List<InstructorReadWindow> windows, Set<UUID> classIds, Set<UUID> problemIds) {
  public AssignmentMetricScope {
    windows = List.copyOf(windows);
    classIds = Set.copyOf(classIds);
    problemIds = Set.copyOf(problemIds);
  }
}
