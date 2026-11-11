package com.algoprep.interviewbank.application.query;

import com.algoprep.common.exception.ResourceNotFoundException;
import com.algoprep.interviewbank.application.ports.in.GetAdminQuestionUseCase;
import com.algoprep.interviewbank.domain.model.AdminQuestion;
import com.algoprep.interviewbank.domain.ports.out.AdminQuestionRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class GetAdminQuestionQueryHandler implements GetAdminQuestionUseCase {
  private final AdminQuestionRepository repository;

  GetAdminQuestionQueryHandler(AdminQuestionRepository repository) {
    this.repository = repository;
  }

  @Override
  @Transactional(readOnly = true)
  public AdminQuestion get(UUID id) {
    return repository
        .findActive(id)
        .orElseThrow(() -> new ResourceNotFoundException("INB-010", "Question not available"));
  }
}
