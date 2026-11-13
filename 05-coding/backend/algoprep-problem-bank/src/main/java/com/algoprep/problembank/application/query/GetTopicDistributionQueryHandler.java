package com.algoprep.problembank.application.query;

import com.algoprep.problembank.application.dto.AuthoringScope;
import com.algoprep.problembank.application.ports.in.GetTopicDistributionUseCase;
import com.algoprep.problembank.domain.model.TopicDistribution;
import com.algoprep.problembank.domain.ports.out.ProblemAuthoringListRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class GetTopicDistributionQueryHandler implements GetTopicDistributionUseCase {
  private final ProblemAuthoringListRepository repository;

  GetTopicDistributionQueryHandler(ProblemAuthoringListRepository repository) {
    this.repository = repository;
  }

  @Override
  @Transactional(readOnly = true)
  public TopicDistribution get(AuthoringScope scope) {
    return repository.topicDistribution(scope.authorFilter());
  }
}
