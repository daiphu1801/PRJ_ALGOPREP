package com.algoprep.problembank.application.query;

import com.algoprep.problembank.application.dto.AuthoringScope;
import com.algoprep.problembank.application.ports.in.GetAttentionListUseCase;
import com.algoprep.problembank.domain.model.AttentionCount;
import com.algoprep.problembank.domain.ports.out.ProblemAuthoringListRepository;
import java.util.List;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Thresholds come from configuration: 30 percent AC and 7 days of a stale draft by default. */
@Service
class GetAttentionListQueryHandler implements GetAttentionListUseCase {
  private static final String PREFIX = "algoprep.problem-bank.attention.";

  private final ProblemAuthoringListRepository repository;
  private final Environment environment;

  GetAttentionListQueryHandler(ProblemAuthoringListRepository repository, Environment environment) {
    this.repository = repository;
    this.environment = environment;
  }

  @Override
  @Transactional(readOnly = true)
  public List<AttentionCount> get(AuthoringScope scope) {
    return repository.attention(
        scope.authorFilter(),
        environment.getProperty(PREFIX + "low-ac-percent", Integer.class, 30),
        environment.getProperty(PREFIX + "stale-draft-days", Integer.class, 7));
  }
}
