package com.algoprep.judge.domain.ports.out;

import com.algoprep.judge.domain.model.WorkerRecords.Outcome;
import com.algoprep.judge.domain.model.WorkerRecords.Result;
import com.algoprep.judge.domain.model.WorkerRecords.State;
import com.algoprep.judge.domain.model.WorkerRecords.TestcaseWrite;
import com.algoprep.judge.domain.model.WorkerRecords.Token;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Each method runs inside the caller's short transaction, never around an engine call. */
public interface SubmissionWorkerRepository {
  Optional<State> compiling(UUID id, int specVersion);

  Optional<State> judging(UUID id, int total);

  Optional<State> heartbeat(UUID id);

  Optional<State> lock(UUID id);

  boolean resultExists(UUID id, UUID testcaseId);

  boolean issue(UUID id, UUID testcaseId, Token token);

  void save(TestcaseWrite write, State state, UUID resultId);

  List<Result> results(UUID id);

  State finish(State before, Outcome outcome);

  boolean firstAttempt(State state);

  void append(UUID eventId, State state, String type, String envelope);
}
