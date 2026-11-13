package com.algoprep.interviewbank.application.query;

import com.algoprep.common.exception.ResourceNotFoundException;
import com.algoprep.interviewbank.application.ports.in.ListMyAnswerAttemptsUseCase;
import com.algoprep.interviewbank.domain.model.AttemptPage;
import com.algoprep.interviewbank.domain.model.PracticePolicy;
import com.algoprep.interviewbank.domain.ports.out.PracticeRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** DD api F6 section 7: the caller's own attempts of an ACTIVE question, newest first. */
@Service
class ListMyAnswerAttemptsQueryHandler implements ListMyAnswerAttemptsUseCase {
  private final PracticeRepository repository;

  ListMyAnswerAttemptsQueryHandler(PracticeRepository repository) {
    this.repository = repository;
  }

  @Override
  @Transactional(readOnly = true)
  public AttemptPage list(UUID userId, UUID questionId, Integer before) {
    if (repository.findActiveQuestion(questionId).isEmpty()) {
      throw new ResourceNotFoundException("INB-010", "Question not available");
    }
    return repository.attempts(userId, questionId, before, PracticePolicy.ATTEMPTS_PAGE_SIZE);
  }
}
