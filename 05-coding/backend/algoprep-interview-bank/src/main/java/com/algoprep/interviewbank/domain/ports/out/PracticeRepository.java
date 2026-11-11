package com.algoprep.interviewbank.domain.ports.out;

import com.algoprep.interviewbank.domain.model.AnswerAttempt;
import com.algoprep.interviewbank.domain.model.AttemptPage;
import com.algoprep.interviewbank.domain.model.NewAttempt;
import com.algoprep.interviewbank.domain.model.PracticeQuestion;
import com.algoprep.interviewbank.domain.model.QuestionLock;
import java.util.Optional;
import java.util.UUID;

/** Practice data. Attempts are only ever inserted and read; there is no update or delete. */
public interface PracticeRepository {
  /** ACTIVE question with its rubric in order; empty for a missing or RETIRED question. */
  Optional<PracticeQuestion> findActiveQuestion(UUID questionId);

  /** {@code SELECT ... FOR SHARE}: waits for a concurrent topic change or retire. */
  Optional<QuestionLock> lockQuestion(UUID questionId);

  int nextAttemptNo(UUID userId, UUID questionId);

  /** Throws {@code AttemptNumberConflictException} on a duplicate attempt number. */
  AnswerAttempt insertAttempt(NewAttempt attempt, int attemptNo);

  /** Upsert of the derived progress row; the counter only grows on the first attempt. */
  void touchHistory(UUID userId, UUID topicId, boolean firstAttemptOfQuestion);

  AttemptPage attempts(UUID userId, UUID questionId, Integer before, int limit);
}
