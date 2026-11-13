package com.algoprep.judge.application.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.algoprep.common.exception.InvalidRequestException;
import com.algoprep.judge.application.dto.MySubmissionMetricQuery;
import com.algoprep.judge.application.ports.out.CurrentMetricsStudentPort;
import com.algoprep.judge.application.ports.out.ProblemTopicReadPort;
import com.algoprep.judge.domain.exception.MySubmissionMetricsUnavailableException;
import com.algoprep.judge.domain.model.SubmissionMetricRange;
import com.algoprep.judge.domain.model.TopicSubmissionCount;
import com.algoprep.judge.domain.ports.out.MySubmissionMetricsRepository;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

class GetMySubmissionMetricsQueryHandlerTest {
  private final MySubmissionMetricsRepository repository =
      mock(MySubmissionMetricsRepository.class);
  private final CurrentMetricsStudentPort principal = mock(CurrentMetricsStudentPort.class);
  private final ProblemTopicReadPort topics = mock(ProblemTopicReadPort.class);
  private final GetMySubmissionMetricsQueryHandler handler =
      new GetMySubmissionMetricsQueryHandler(repository, principal, topics);
  private final UUID student = UUID.randomUUID();
  private final Instant now = Instant.parse("2026-11-04T05:00:00Z");

  @BeforeEach
  void fixture() {
    when(principal.requireStudent()).thenReturn(student);
    when(repository.now()).thenReturn(now);
    when(repository.days(any(), any(), any())).thenReturn(List.of());
  }

  @Test
  void noRequestedTopicsAvoidsCallingProblemBankAndUsesOnlyTheCurrentStudent() {
    var result =
        handler.read(new MySubmissionMetricQuery(SubmissionMetricRange.SEVEN_DAYS, Set.of()));
    assertThat(result.dailyCounts()).hasSize(7);
    assertThat(result.acRateByTopic()).isNull();
    verify(repository).days(student, Instant.parse("2026-10-28T17:00:00Z"), now);
    verifyNoInteractions(topics);
  }

  @Test
  void topicReadsApplyTheRangeAndKeepMissingCountsExplicitlyUnavailable() {
    UUID topic = UUID.randomUUID();
    UUID problem = UUID.randomUUID();
    var map = Map.of(topic, Set.of(problem));
    when(topics.getProblemIdsByTopics(Set.of(topic))).thenReturn(map);
    when(repository.topics(student, map, Instant.parse("2026-10-05T17:00:00Z"), now))
        .thenReturn(Map.of(topic, new TopicSubmissionCount(2, 1, now)));
    var result =
        handler.read(new MySubmissionMetricQuery(SubmissionMetricRange.THIRTY_DAYS, Set.of(topic)));
    assertThat(result.acRateByTopic().getFirst().acRate()).isEqualTo(.5);
    assertThat(result.acRateByTopic().getFirst().lastSubmittedAt()).isEqualTo(now);
    when(topics.getProblemIdsByTopics(Set.of(topic))).thenReturn(Map.of());
    assertThatThrownBy(
            () ->
                handler.read(new MySubmissionMetricQuery(SubmissionMetricRange.ALL, Set.of(topic))))
        .isInstanceOf(MySubmissionMetricsUnavailableException.class)
        .hasNoCause();
  }

  @Test
  void p5UsesSuppliedMapAndNeverCallsBackToProblemBank() {
    UUID topic = UUID.randomUUID();
    var map = Map.of(topic, Set.<UUID>of());
    when(repository.topics(student, map, null, now))
        .thenReturn(Map.of(topic, new TopicSubmissionCount(0, 0, null)));
    var result = handler.readTopics(map);
    assertThat(result.keySet()).containsExactly(topic);
    assertThat(result.get(topic).acRate()).isNull();
    verifyNoInteractions(topics);
  }

  @Test
  void invalidSizesAndAbsentStudentCannotQueryData() {
    Map<UUID, Set<UUID>> many = new LinkedHashMap<>();
    for (int i = 0; i < 51; i++) {
      many.put(UUID.randomUUID(), Set.of());
    }
    assertThatThrownBy(() -> handler.readTopics(many)).isInstanceOf(InvalidRequestException.class);
    assertThatThrownBy(() -> new MySubmissionMetricQuery(null, Set.of()))
        .isInstanceOf(InvalidRequestException.class);
    assertThatThrownBy(() -> new MySubmissionMetricQuery(SubmissionMetricRange.ALL, many.keySet()))
        .isInstanceOf(InvalidRequestException.class);
    when(principal.requireStudent()).thenThrow(new AccessDeniedException("denied"));
    assertThatThrownBy(
            () -> handler.read(new MySubmissionMetricQuery(SubmissionMetricRange.ALL, Set.of())))
        .isInstanceOf(AccessDeniedException.class);
    verify(repository, never()).now();
  }

  @Test
  void databaseAndP5FailuresHaveNoRawMessageCauseOrFakeZeros() {
    when(repository.now()).thenThrow(new IllegalStateException("private SQL"));
    assertThatThrownBy(
            () -> handler.read(new MySubmissionMetricQuery(SubmissionMetricRange.ALL, Set.of())))
        .isInstanceOf(MySubmissionMetricsUnavailableException.class)
        .hasMessage("Submission metrics are temporarily unavailable")
        .hasNoCause();
    assertThatThrownBy(() -> handler.readTopics(Map.of(UUID.randomUUID(), Set.of())))
        .isInstanceOf(MySubmissionMetricsUnavailableException.class)
        .hasNoCause();
  }
}
