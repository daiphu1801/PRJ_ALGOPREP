package com.algoprep.judge.application.ports.out;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

public interface ProblemTopicReadPort {
  Map<UUID, Set<UUID>> getProblemIdsByTopics(Set<UUID> topicIds);
}
