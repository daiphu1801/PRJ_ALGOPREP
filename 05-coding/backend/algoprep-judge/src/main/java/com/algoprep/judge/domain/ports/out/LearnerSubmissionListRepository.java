package com.algoprep.judge.domain.ports.out;

import com.algoprep.judge.domain.model.LearnerSubmissionCandidate;
import java.util.List;
import java.util.UUID;

public interface LearnerSubmissionListRepository {
  List<LearnerSubmissionCandidate> inProgress(
      UUID student, LearnerSubmissionCandidate.Cursor after, int batchSize);

  List<LearnerSubmissionCandidate> accepted(UUID student, int limit);
}
