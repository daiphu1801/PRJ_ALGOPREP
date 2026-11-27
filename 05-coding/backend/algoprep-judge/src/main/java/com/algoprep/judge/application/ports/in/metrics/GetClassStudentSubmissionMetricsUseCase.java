package com.algoprep.judge.application.ports.in.metrics;

import com.algoprep.judge.domain.model.metrics.ClassMetricsScope;
import com.algoprep.judge.domain.model.metrics.StudentSubmissionMetrics;
import java.util.List;

/** Internal trusted caller only; identity enforces class permissions before supplying scope. */
public interface GetClassStudentSubmissionMetricsUseCase {
  List<StudentSubmissionMetrics> read(ClassMetricsScope scope);
}
