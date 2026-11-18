package com.algoprep.judge.application.ports.in.execution;

import com.algoprep.judge.domain.model.execution.WorkerRecords.Token;
import java.util.Optional;
import java.util.UUID;

/** Internal trusted worker/sweep boundary. Public callers cannot supply grading writes. */
public interface IssueSubmissionRunTokenUseCase {
  Optional<Token> issue(UUID id, UUID testcaseId);
}
