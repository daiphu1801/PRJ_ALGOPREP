package com.algoprep.interviewbank.application.query;

import com.algoprep.common.exception.ResourceNotFoundException;
import com.algoprep.interviewbank.application.ports.in.GetInterviewQuestionUseCase;
import com.algoprep.interviewbank.domain.model.LearnerQuestion;
import com.algoprep.interviewbank.domain.ports.out.LearnerQuestionRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Study mode: static content only, never an AI call (BR-IB-05). */
@Service
class GetInterviewQuestionQueryHandler implements GetInterviewQuestionUseCase {
  private final LearnerQuestionRepository repository;

  GetInterviewQuestionQueryHandler(LearnerQuestionRepository repository) {
    this.repository = repository;
  }

  @Override
  @Transactional(readOnly = true)
  public LearnerQuestion get(UUID userId, UUID questionId) {
    return repository
        .find(userId, questionId)
        .orElseThrow(() -> new ResourceNotFoundException("INB-010", "Question not available"));
  }
}
