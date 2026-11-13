package com.algoprep.judge.application.ports.out;

import com.algoprep.judge.domain.model.ClassMetricsScope;
import com.algoprep.judge.domain.model.StudentSubmissionMetrics;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ClassSubmissionMetricsCache {
  Optional<List<StudentSubmissionMetrics>> get(ClassMetricsScope scope, LocalDate weekStart);

  void put(ClassMetricsScope scope, LocalDate weekStart, List<StudentSubmissionMetrics> metrics);
}
