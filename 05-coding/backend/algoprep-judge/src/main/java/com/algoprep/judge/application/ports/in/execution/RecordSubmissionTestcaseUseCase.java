package com.algoprep.judge.application.ports.in;

import com.algoprep.judge.domain.model.WorkerRecords.TestcaseWrite;

/** Internal trusted worker/sweep boundary. Public callers cannot supply grading writes. */
public interface RecordSubmissionTestcaseUseCase {
  boolean record(TestcaseWrite write);
}
