package com.algoprep.judge.application.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.algoprep.judge.application.ports.out.ClassSubmissionMetricsCache;
import com.algoprep.judge.domain.model.ClassMetricSubmission;
import com.algoprep.judge.domain.model.ClassMetricsScope;
import com.algoprep.judge.domain.model.StudentSubmissionMetrics;
import com.algoprep.judge.domain.ports.out.ClassSubmissionMetricsRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GetClassStudentSubmissionMetricsQueryHandlerTest {
  private final ClassSubmissionMetricsRepository repository =
      mock(ClassSubmissionMetricsRepository.class);
  private final ClassSubmissionMetricsCache cache = mock(ClassSubmissionMetricsCache.class);
  private final GetClassStudentSubmissionMetricsQueryHandler handler =
      new GetClassStudentSubmissionMetricsQueryHandler(repository, cache);
  private final Instant now = Instant.parse("2026-11-04T05:00:00Z");
  private final UUID student = UUID.randomUUID();
  private final UUID problem = UUID.randomUUID();
  private final ClassMetricsScope scope =
      new ClassMetricsScope(
          UUID.randomUUID(),
          List.of(new ClassMetricsScope.Student(student, now.minusSeconds(86400), null)),
          List.of(new ClassMetricsScope.Assignment(problem, now.minusSeconds(86400), null)),
          2);

  @BeforeEach
  void clock() {
    when(repository.now()).thenReturn(now);
  }

  @Test
  void cacheHitReturnsWithoutReadingSubmissions() {
    var result =
        List.of(new StudentSubmissionMetrics(student, 0, null, null, List.of(), List.of()));
    when(cache.get(scope, LocalDate.parse("2026-11-02"))).thenReturn(Optional.of(result));
    assertThat(handler.read(scope)).isSameAs(result);
    verify(repository, never()).find(any(), any());
  }

  @Test
  void redisReadAndWriteFailuresDoNotHideAuthoritativeResults() {
    when(cache.get(any(), any())).thenThrow(new IllegalStateException("private Redis"));
    org.mockito.Mockito.doThrow(new IllegalStateException("private Redis"))
        .when(cache)
        .put(any(), any(), any());
    when(repository.find(scope, now))
        .thenReturn(
            List.of(
                new ClassMetricSubmission(
                    UUID.randomUUID(), student, problem, "ACCEPTED", BigDecimal.ONE, now)));
    var result = handler.read(scope).getFirst();
    assertThat(result.acceptedRatio()).isEqualTo(1.0);
    assertThat(result.streakDays()).isEqualTo(1);
    assertThat(result.assignmentResults().getFirst().accepted()).isTrue();
    verify(repository).find(scope, now);
  }

  @Test
  void cachedForeignStudentCannotReplaceTheRequestedScope() {
    var foreign =
        new StudentSubmissionMetrics(UUID.randomUUID(), 9, now, 1.0, List.of(), List.of());
    when(cache.get(any(), any())).thenReturn(Optional.of(List.of(foreign)));
    when(repository.find(scope, now)).thenReturn(List.of());
    assertThat(handler.read(scope).getFirst().studentId()).isEqualTo(student);
    assertThat(handler.read(scope).getFirst().streakDays()).isZero();
  }

  @Test
  void dependencyFailureHasAFixedMessageWithoutCauseOrFalseZeros() {
    when(repository.now()).thenThrow(new IllegalStateException("private SQL password"));
    assertThatThrownBy(() -> handler.read(scope))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Class submission metrics are temporarily unavailable")
        .hasNoCause();
    verify(cache, never()).get(any(), any());
  }
}
