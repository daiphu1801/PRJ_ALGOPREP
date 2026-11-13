package com.algoprep.bootstrap.judge;

import com.algoprep.judge.application.ports.out.ProblemTopicReadPort;
import com.algoprep.problembank.application.ports.in.ReadHistoricalTopicProblemIdsUseCase;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
class ProblemTopicReadBridge implements ProblemTopicReadPort {
  private final ReadHistoricalTopicProblemIdsUseCase topics;

  ProblemTopicReadBridge(ReadHistoricalTopicProblemIdsUseCase topics) {
    this.topics = topics;
  }

  @Override
  public Map<UUID, Set<UUID>> getProblemIdsByTopics(Set<UUID> topicIds) {
    return topics.read(topicIds);
  }
}
