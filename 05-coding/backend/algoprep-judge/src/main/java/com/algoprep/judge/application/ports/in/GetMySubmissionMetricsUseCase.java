package com.algoprep.judge.application.ports.in;

import com.algoprep.judge.application.dto.MySubmissionMetricQuery;
import com.algoprep.judge.application.dto.MySubmissionMetrics;

public interface GetMySubmissionMetricsUseCase {
  MySubmissionMetrics read(MySubmissionMetricQuery query);
}
