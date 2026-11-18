package com.algoprep.judge.domain.ports.out;

import com.algoprep.judge.domain.model.metrics.AssignmentMetricScope;
import com.algoprep.judge.domain.model.metrics.AssignmentPeriod;
import com.algoprep.judge.domain.model.metrics.AssignmentSolveState;
import com.algoprep.judge.domain.model.metrics.ClassProblemMetric;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface AssignmentMetricsRepository {
  Map<UUID, ClassProblemMetric> problemMetrics(AssignmentMetricScope scope);

  Map<AssignmentPeriod.Key, AssignmentSolveState> assignmentStates(
      UUID student, List<AssignmentPeriod> assignments);
}
