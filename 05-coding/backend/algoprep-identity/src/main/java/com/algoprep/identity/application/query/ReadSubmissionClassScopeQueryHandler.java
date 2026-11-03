package com.algoprep.identity.application.query;

import com.algoprep.identity.application.ports.in.ReadSubmissionClassScopeUseCase;
import com.algoprep.identity.domain.model.SubmissionClassScope;
import com.algoprep.identity.domain.ports.out.SubmissionClassScopeRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
class ReadSubmissionClassScopeQueryHandler implements ReadSubmissionClassScopeUseCase {
  private final SubmissionClassScopeRepository repository;

  ReadSubmissionClassScopeQueryHandler(SubmissionClassScopeRepository repository) {
    this.repository = repository;
  }

  @Override
  public List<SubmissionClassScope> read(UUID instructorId) {
    return repository.read(instructorId);
  }
}
