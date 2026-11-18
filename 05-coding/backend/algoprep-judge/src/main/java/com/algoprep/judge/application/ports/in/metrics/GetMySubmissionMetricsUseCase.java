package com.algoprep.judge.application.ports.in.metrics;

import com.algoprep.judge.application.dto.metrics.MySubmissionMetricQuery;
import com.algoprep.judge.application.dto.metrics.MySubmissionMetrics;

public interface GetMySubmissionMetricsUseCase {
  MySubmissionMetrics read(MySubmissionMetricQuery query);
}
