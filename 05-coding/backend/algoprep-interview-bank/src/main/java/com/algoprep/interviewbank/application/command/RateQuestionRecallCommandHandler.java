package com.algoprep.interviewbank.application.command;

import com.algoprep.common.exception.ResourceNotFoundException;
import com.algoprep.interviewbank.application.ports.in.RateQuestionRecallUseCase;
import com.algoprep.interviewbank.domain.model.RecallRating;
import com.algoprep.interviewbank.domain.model.RecallState;
import com.algoprep.interviewbank.domain.ports.out.LearnerQuestionRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** BR-IB-06: the latest rating overwrites the previous one; no history, no accumulation. */
@Service
class RateQuestionRecallCommandHandler implements RateQuestionRecallUseCase {
  private final LearnerQuestionRepository repository;

  RateQuestionRecallCommandHandler(LearnerQuestionRepository repository) {
    this.repository = repository;
  }

  @Override
  @Transactional
  public RecallState rate(UUID userId, UUID questionId, RecallRating rating) {
    return repository
        .rate(userId, questionId, rating)
        .orElseThrow(() -> new ResourceNotFoundException("INB-010", "Question not available"));
  }
}
