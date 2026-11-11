package com.algoprep.interviewbank.domain.ports.out;

import com.algoprep.interviewbank.domain.model.LearnerQuestion;
import com.algoprep.interviewbank.domain.model.LearnerQuestionCriteria;
import com.algoprep.interviewbank.domain.model.LearnerQuestionPage;
import com.algoprep.interviewbank.domain.model.RecallRating;
import com.algoprep.interviewbank.domain.model.RecallState;
import java.util.Optional;
import java.util.UUID;

/** Every method is scoped by the caller user id; a RETIRED question reads as absent. */
public interface LearnerQuestionRepository {
  LearnerQuestionPage page(LearnerQuestionCriteria criteria);

  Optional<LearnerQuestion> find(UUID userId, UUID questionId);

  /** One-statement upsert; empty when the question is not ACTIVE. Overwrites in place. */
  Optional<RecallState> rate(UUID userId, UUID questionId, RecallRating rating);

  boolean isActive(UUID questionId);

  /** Idempotent: inserting an existing bookmark or deleting a missing one both succeed. */
  void setBookmark(UUID userId, UUID questionId, boolean bookmarked);
}
