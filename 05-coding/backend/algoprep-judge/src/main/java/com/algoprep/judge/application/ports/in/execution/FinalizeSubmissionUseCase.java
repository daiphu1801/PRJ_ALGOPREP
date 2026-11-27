package com.algoprep.judge.application.ports.in.execution;

import com.algoprep.judge.domain.model.execution.WorkerRecords.FinishCommand;
import com.algoprep.judge.domain.model.execution.WorkerRecords.Finished;
import java.util.Optional;

/** Internal trusted worker/sweep boundary. Public callers cannot supply grading writes. */
public interface FinalizeSubmissionUseCase {
  Optional<Finished> finish(FinishCommand command);
}
