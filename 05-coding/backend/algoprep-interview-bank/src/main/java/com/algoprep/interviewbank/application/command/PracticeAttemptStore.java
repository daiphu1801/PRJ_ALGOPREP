package com.algoprep.interviewbank.application.command;

import com.algoprep.common.exception.ResourceNotFoundException;
import com.algoprep.interviewbank.domain.model.AnswerAttempt;
import com.algoprep.interviewbank.domain.model.NewAttempt;
import com.algoprep.interviewbank.domain.model.QuestionLock;
import com.algoprep.interviewbank.domain.ports.out.PracticeRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * The one short transaction of DD api F6 section 6.5 step 9: share-lock the question row, number
 * the attempt, insert it, move the derived progress. A duplicate attempt number rolls the whole
 * block back; the caller retries it with the same answer id and the same AI result.
 */
@Component
class PracticeAttemptStore {
  private final PracticeRepository repository;

  PracticeAttemptStore(PracticeRepository repository) {
    this.repository = repository;
  }

  @Transactional
  AnswerAttempt save(NewAttempt attempt) {
    QuestionLock lock =
        repository
            .lockQuestion(attempt.questionId())
            .orElseThrow(() -> new ResourceNotFoundException("INB-010", "Question not available"));
    int attemptNo = repository.nextAttemptNo(attempt.userId(), attempt.questionId());
    AnswerAttempt saved = repository.insertAttempt(attempt, attemptNo);
    // A question retired while the AI was thinking keeps the attempt but leaves progress alone,
    // because the progress numerator counts ACTIVE questions only (BR-IB-09).
    if (lock.active()) {
      repository.touchHistory(attempt.userId(), lock.topicId(), attemptNo == 1);
    }
    return saved;
  }
}
