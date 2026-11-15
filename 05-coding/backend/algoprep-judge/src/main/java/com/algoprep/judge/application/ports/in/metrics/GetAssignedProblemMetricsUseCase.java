package com.algoprep.judge.application.ports.in;

import com.algoprep.judge.application.dto.AssignedProblemMetricsQuery;
import com.algoprep.judge.domain.model.ClassProblemMetric;
import java.util.Map;
import java.util.UUID;

/** P6: trusted F2 caller checks instructor authority and supplies complete assignment history. */
public interface GetAssignedProblemMetricsUseCase {
  Map<UUID, ClassProblemMetric> read(AssignedProblemMetricsQuery query);
}
