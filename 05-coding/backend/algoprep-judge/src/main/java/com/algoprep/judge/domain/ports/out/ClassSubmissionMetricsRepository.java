package com.algoprep.judge.domain.ports.out;

import com.algoprep.judge.domain.model.metrics.ClassMetricSubmission;
import com.algoprep.judge.domain.model.metrics.ClassMetricsScope;
import java.time.Instant;
import java.util.List;

public interface ClassSubmissionMetricsRepository {
  Instant now();

  List<ClassMetricSubmission> find(ClassMetricsScope scope, Instant asOf);
}
