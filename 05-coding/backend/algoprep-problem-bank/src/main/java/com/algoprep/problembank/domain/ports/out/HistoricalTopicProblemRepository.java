package com.algoprep.problembank.domain.ports.out;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

public interface HistoricalTopicProblemRepository {
  Map<UUID, Set<UUID>> read(Set<UUID> topicIds);
}
