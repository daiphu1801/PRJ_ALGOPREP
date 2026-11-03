package com.algoprep.judge.application.ports.in;

import com.algoprep.judge.domain.model.ClassMetricsScope;
import com.algoprep.judge.domain.model.StudentSubmissionMetrics;
import java.util.List;

/** Internal trusted caller only; identity enforces class permissions before supplying scope. */
public interface GetClassStudentSubmissionMetricsUseCase {
  List<StudentSubmissionMetrics> read(ClassMetricsScope scope);
}
