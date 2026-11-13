package com.algoprep.judge.application.command;

import com.algoprep.common.domain.ports.out.IdGeneratorPort;
import com.algoprep.judge.application.dto.SubmissionFinishedProgress;
import com.algoprep.judge.application.ports.in.FinalizeSubmissionUseCase;
import com.algoprep.judge.application.ports.out.SubmissionProgressPort;
import com.algoprep.judge.domain.model.WorkerRecords.FinishCommand;
import com.algoprep.judge.domain.model.WorkerRecords.Finished;
import com.algoprep.judge.domain.ports.out.SubmissionWorkerRepository;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
class FinalizeSubmissionCommandHandler implements FinalizeSubmissionUseCase {
  private final SubmissionWorkerRepository repository;
  private final SubmissionProgressPort progress;
  private final WorkerTransactions transactions;
  private final WorkerSubmissionEvents events;

  FinalizeSubmissionCommandHandler(
      SubmissionWorkerRepository repository,
      SubmissionProgressPort progress,
      WorkerTransactions transactions,
      IdGeneratorPort ids) {
    this.repository = repository;
    this.progress = progress;
    this.transactions = transactions;
    this.events = new WorkerSubmissionEvents(repository, ids);
  }

  @Override
  public Optional<Finished> finish(FinishCommand command) {
    Objects.requireNonNull(command);
    return transactions.execute(() -> finishLocked(command));
  }

  /** Caller must own a write transaction; sweep also holds the job and staleness guards. */
  Optional<Finished> finishLocked(FinishCommand command) {
    var locked = repository.lock(command.submissionId());
    if (locked.isEmpty()) {
      return Optional.empty();
    }
    var before = locked.orElseThrow();
    if (!java.util.Set.of("PENDING", "COMPILING", "JUDGING").contains(before.status())) {
      return Optional.empty();
    }
    var outcome =
        SubmissionFinalizationPolicy.summarize(before, command, repository.results(before.id()));
    var saved = repository.finish(before, outcome);
    var finished = new Finished(saved, outcome, before.total());
    events.append(finished);
    progress.afterCommit(
        saved.owner().userId(),
        new SubmissionFinishedProgress(
            saved.id(),
            saved.updatedAt(),
            saved.status(),
            outcome.passed(),
            outcome.total(),
            outcome.score(),
            outcome.firstFailingOrder(),
            outcome.runtime() == null ? null : outcome.runtime().longValue(),
            outcome.memory() == null ? null : outcome.memory().longValue(),
            saved.updatedAt(),
            before.total()));
    return Optional.of(finished);
  }
}
