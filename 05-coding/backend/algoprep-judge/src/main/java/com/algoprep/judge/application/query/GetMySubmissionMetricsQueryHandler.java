package com.algoprep.judge.application.query;

import com.algoprep.common.exception.InvalidRequestException;
import com.algoprep.judge.application.dto.MySubmissionMetricQuery;
import com.algoprep.judge.application.dto.MySubmissionMetrics;
import com.algoprep.judge.application.dto.MySubmissionMetrics.TopicMetric;
import com.algoprep.judge.application.dto.MySubmissionMetrics.TopicRatio;
import com.algoprep.judge.application.ports.in.GetMySubmissionMetricsUseCase;
import com.algoprep.judge.application.ports.in.GetMyTopicSubmissionMetricsUseCase;
import com.algoprep.judge.application.ports.out.CurrentMetricsStudentPort;
import com.algoprep.judge.application.ports.out.ProblemTopicReadPort;
import com.algoprep.judge.domain.exception.MySubmissionMetricsUnavailableException;
import com.algoprep.judge.domain.metrics.MySubmissionMetricsCalculator;
import com.algoprep.judge.domain.ports.out.MySubmissionMetricsRepository;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
class GetMySubmissionMetricsQueryHandler
    implements GetMySubmissionMetricsUseCase, GetMyTopicSubmissionMetricsUseCase {
  private final MySubmissionMetricsRepository repository;
  private final CurrentMetricsStudentPort principal;
  private final ProblemTopicReadPort topics;

  GetMySubmissionMetricsQueryHandler(
      MySubmissionMetricsRepository repository,
      CurrentMetricsStudentPort principal,
      ProblemTopicReadPort topics) {
    this.repository = repository;
    this.principal = principal;
    this.topics = topics;
  }

  @Override
  public MySubmissionMetrics read(MySubmissionMetricQuery query) {
    UUID student = principal.requireStudent();
    try {
      var now = repository.now();
      var from = MySubmissionMetricsCalculator.from(query.range(), now);
      return new MySubmissionMetrics(
          repository.streak(student, now),
          repository.last(student, now),
          MySubmissionMetricsCalculator.buckets(
              query.range(), repository.days(student, from, now), now),
          topicMetrics(query.topicIds(), student, from, now));
    } catch (RuntimeException failure) {
      throw new MySubmissionMetricsUnavailableException();
    }
  }

  private List<TopicMetric> topicMetrics(
      Set<UUID> requested, UUID student, Instant from, Instant now) {
    if (requested.isEmpty()) {
      return null;
    }
    var links = topics.getProblemIdsByTopics(requested);
    if (!links.keySet().containsAll(requested)) {
      throw new IllegalStateException("Incomplete topic mapping");
    }
    Map<UUID, Set<UUID>> selected = new LinkedHashMap<>();
    requested.forEach(topic -> selected.put(topic, links.get(topic)));
    var counts = repository.topics(student, selected, from, now);
    return requested.stream()
        .sorted(java.util.Comparator.comparing(UUID::toString))
        .map(
            topic -> {
              var row = Objects.requireNonNull(counts.get(topic));
              return new TopicMetric(
                  topic,
                  row.submissionCount(),
                  row.acceptedCount(),
                  row.acRate(),
                  row.lastSubmittedAt());
            })
        .toList();
  }

  @Override
  public Map<UUID, TopicRatio> readTopics(Map<UUID, Set<UUID>> topicProblemIds) {
    if (topicProblemIds.size() > 50) {
      throw new InvalidRequestException("JDG-V15", "Invalid metrics topics");
    }
    Map<UUID, Set<UUID>> scope = new LinkedHashMap<>();
    topicProblemIds.forEach(
        (id, problems) -> scope.put(Objects.requireNonNull(id), Set.copyOf(problems)));
    UUID student = principal.requireStudent();
    try {
      var counts = repository.topics(student, scope, null, repository.now());
      Map<UUID, TopicRatio> result = new LinkedHashMap<>();
      scope
          .keySet()
          .forEach(
              id -> {
                var row = Objects.requireNonNull(counts.get(id));
                result.put(
                    id, new TopicRatio(row.submissionCount(), row.acceptedCount(), row.acRate()));
              });
      return Map.copyOf(result);
    } catch (RuntimeException failure) {
      throw new MySubmissionMetricsUnavailableException();
    }
  }
}
