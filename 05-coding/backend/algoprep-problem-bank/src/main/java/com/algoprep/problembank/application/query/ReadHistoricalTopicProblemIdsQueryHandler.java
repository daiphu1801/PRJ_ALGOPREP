package com.algoprep.problembank.application.query;

import com.algoprep.problembank.application.ports.in.ReadHistoricalTopicProblemIdsUseCase;
import com.algoprep.problembank.domain.ports.out.HistoricalTopicProblemRepository;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class ReadHistoricalTopicProblemIdsQueryHandler implements ReadHistoricalTopicProblemIdsUseCase {
  private final HistoricalTopicProblemRepository repository;

  ReadHistoricalTopicProblemIdsQueryHandler(HistoricalTopicProblemRepository repository) {
    this.repository = repository;
  }

  @Override
  @Transactional(readOnly = true)
  public Map<UUID, Set<UUID>> read(Set<UUID> topicIds) {
    var scope = Set.copyOf(topicIds);
    if (scope.size() > 50) {
      throw new IllegalArgumentException("Too many metric topics");
    }
    return repository.read(scope);
  }
}
