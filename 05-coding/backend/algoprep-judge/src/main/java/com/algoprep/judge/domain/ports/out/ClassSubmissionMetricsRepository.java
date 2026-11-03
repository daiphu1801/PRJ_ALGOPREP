package com.algoprep.judge.domain.ports.out;

import com.algoprep.judge.domain.model.ClassMetricSubmission;
import com.algoprep.judge.domain.model.ClassMetricsScope;
import java.time.Instant;
import java.util.List;

public interface ClassSubmissionMetricsRepository {
  Instant now();

  List<ClassMetricSubmission> find(ClassMetricsScope scope, Instant asOf);
}
