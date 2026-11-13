package com.algoprep.judge.application.query;

import com.algoprep.judge.application.ports.in.ReadGradingDefaultsUseCase;
import com.algoprep.judge.domain.model.GradingDefaults;
import com.algoprep.judge.domain.ports.out.GradingDefaultsRepository;
import org.springframework.stereotype.Service;

/** No cache: each invocation observes one database snapshot of all four limits. */
@Service
public final class GetGradingDefaultsQueryHandler implements ReadGradingDefaultsUseCase {
  private final GradingDefaultsRepository repository;

  public GetGradingDefaultsQueryHandler(GradingDefaultsRepository repository) {
    this.repository = repository;
  }

  @Override
  public GradingDefaults getGradingDefaults() {
    return repository.read();
  }
}
