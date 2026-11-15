package com.algoprep.judge.application.ports.in;

import com.algoprep.judge.domain.model.WorkerRecords.State;
import java.util.Optional;
import java.util.UUID;

/** Internal trusted worker/sweep boundary. Public callers cannot supply grading writes. */
public interface BeginSubmissionJudgingUseCase {
  Optional<State> beginJudging(UUID id, int total);
}
