package com.algoprep.judge.domain.ports.out;

import com.algoprep.judge.domain.model.SubmissionActivityDay;
import com.algoprep.judge.domain.model.TopicSubmissionCount;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public interface MySubmissionMetricsRepository {
  Instant now();

  int streak(UUID student, Instant asOf);

  Instant last(UUID student, Instant asOf);

  List<SubmissionActivityDay> days(UUID student, Instant from, Instant asOf);

  Map<UUID, TopicSubmissionCount> topics(
      UUID student, Map<UUID, Set<UUID>> links, Instant from, Instant asOf);
}
