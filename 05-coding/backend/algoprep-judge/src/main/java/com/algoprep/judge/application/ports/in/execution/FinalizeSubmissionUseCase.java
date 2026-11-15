package com.algoprep.judge.application.ports.in;

import com.algoprep.judge.domain.model.WorkerRecords.FinishCommand;
import com.algoprep.judge.domain.model.WorkerRecords.Finished;
import java.util.Optional;

/** Internal trusted worker/sweep boundary. Public callers cannot supply grading writes. */
public interface FinalizeSubmissionUseCase {
  Optional<Finished> finish(FinishCommand command);
}
