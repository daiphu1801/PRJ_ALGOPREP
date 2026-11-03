package com.algoprep.judge.domain.ports.out;

import com.algoprep.judge.domain.model.PendingSubmission;
import com.algoprep.judge.domain.model.PendingSubmission.Owner;
import java.time.Instant;

/**
 * All lock/count/insert calls use the same READ COMMITTED transaction. Failures are fail-closed.
 */
public interface SubmissionAdmissionRepository {
  boolean languageEnabled(String language);

  void lockPair(Owner owner);

  Instant databaseNow();

  long countRecent(Owner owner, Instant at);

  void insert(PendingSubmission submission);
}
