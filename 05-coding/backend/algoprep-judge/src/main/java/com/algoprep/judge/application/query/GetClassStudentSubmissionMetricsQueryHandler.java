package com.algoprep.judge.application.query;

import com.algoprep.judge.application.ports.in.GetClassStudentSubmissionMetricsUseCase;
import com.algoprep.judge.application.ports.out.ClassSubmissionMetricsCache;
import com.algoprep.judge.domain.metrics.ClassSubmissionMetricsCalculator;
import com.algoprep.judge.domain.model.ClassMetricsScope;
import com.algoprep.judge.domain.model.StudentSubmissionMetrics;
import com.algoprep.judge.domain.ports.out.ClassSubmissionMetricsRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
class GetClassStudentSubmissionMetricsQueryHandler
    implements GetClassStudentSubmissionMetricsUseCase {
  private final ClassSubmissionMetricsRepository repository;
  private final ClassSubmissionMetricsCache cache;

  GetClassStudentSubmissionMetricsQueryHandler(
      ClassSubmissionMetricsRepository repository, ClassSubmissionMetricsCache cache) {
    this.repository = repository;
    this.cache = cache;
  }

  @Override
  @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
  public List<StudentSubmissionMetrics> read(ClassMetricsScope scope) {
    try {
      var now = repository.now();
      var week = ClassSubmissionMetricsCalculator.weekStart(now);
      var cached = cached(scope, week);
      if (cached.isPresent()) {
        return cached.orElseThrow();
      }
      var result =
          ClassSubmissionMetricsCalculator.calculate(scope, repository.find(scope, now), now);
      store(scope, week, result);
      return result;
    } catch (RuntimeException unavailable) {
      throw new IllegalStateException("Class submission metrics are temporarily unavailable");
    }
  }

  private Optional<List<StudentSubmissionMetrics>> cached(ClassMetricsScope scope, LocalDate week) {
    try {
      var result = cache.get(scope, week);
      return result.filter(
          items ->
              items.stream()
                  .map(StudentSubmissionMetrics::studentId)
                  .toList()
                  .equals(
                      scope.students().stream()
                          .map(ClassMetricsScope.Student::studentId)
                          .toList()));
    } catch (RuntimeException cacheUnavailable) {
      return Optional.empty();
    }
  }

  private void store(
      ClassMetricsScope scope, LocalDate week, List<StudentSubmissionMetrics> result) {
    try {
      cache.put(scope, week, result);
    } catch (RuntimeException cacheUnavailable) {
      // Cache is optional; authoritative DB results remain available.
    }
  }
}
