package com.algoprep.interviewbank.application.command;

import com.algoprep.common.exception.ResourceNotFoundException;
import com.algoprep.interviewbank.application.ports.in.SetQuestionBookmarkUseCase;
import com.algoprep.interviewbank.domain.ports.out.LearnerQuestionRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Sets the wanted state instead of toggling, so two quick clicks cannot cancel each other. */
@Service
class SetQuestionBookmarkCommandHandler implements SetQuestionBookmarkUseCase {
  private final LearnerQuestionRepository repository;

  SetQuestionBookmarkCommandHandler(LearnerQuestionRepository repository) {
    this.repository = repository;
  }

  @Override
  @Transactional
  public void set(UUID userId, UUID questionId, boolean bookmarked) {
    if (!repository.isActive(questionId)) {
      throw new ResourceNotFoundException("INB-010", "Question not available");
    }
    repository.setBookmark(userId, questionId, bookmarked);
  }
}
