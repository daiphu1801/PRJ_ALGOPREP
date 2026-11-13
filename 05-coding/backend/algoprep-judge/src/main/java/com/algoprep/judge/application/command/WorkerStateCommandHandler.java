package com.algoprep.judge.application.command;

import com.algoprep.judge.application.dto.SubmissionStatusProgress;
import com.algoprep.judge.application.ports.in.BeginSubmissionCompilationUseCase;
import com.algoprep.judge.application.ports.in.BeginSubmissionJudgingUseCase;
import com.algoprep.judge.application.ports.out.SubmissionProgressPort;
import com.algoprep.judge.domain.model.WorkerRecords.State;
import com.algoprep.judge.domain.ports.out.SubmissionWorkerRepository;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
class WorkerStateCommandHandler
    implements BeginSubmissionCompilationUseCase, BeginSubmissionJudgingUseCase {
  private final SubmissionWorkerRepository repository;
  private final SubmissionProgressPort progress;
  private final WorkerTransactions transactions;

  WorkerStateCommandHandler(
      SubmissionWorkerRepository repository,
      SubmissionProgressPort progress,
      WorkerTransactions transactions) {
    this.repository = repository;
    this.progress = progress;
    this.transactions = transactions;
  }

  @Override
  public Optional<State> begin(UUID id, int specVersion) {
    Objects.requireNonNull(id);
    if (specVersion < 1) {
      throw new IllegalArgumentException("Invalid spec version");
    }
    return transactions.execute(() -> notify(repository.compiling(id, specVersion)));
  }

  @Override
  public Optional<State> beginJudging(UUID id, int total) {
    Objects.requireNonNull(id);
    if (total < 1 || total > Integer.MAX_VALUE - 3) {
      throw new IllegalArgumentException("Invalid testcase count");
    }
    return transactions.execute(() -> notify(repository.judging(id, total)));
  }

  private Optional<State> notify(Optional<State> changed) {
    changed.ifPresent(
        s ->
            progress.afterCommit(
                s.owner().userId(),
                new SubmissionStatusProgress(s.id(), s.updatedAt(), s.status(), s.total())));
    return changed;
  }
}
