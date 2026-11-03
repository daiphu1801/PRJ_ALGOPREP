package com.algoprep.judge.application.command;

import com.algoprep.judge.application.ports.in.SweepStaleSubmissionsUseCase;
import com.algoprep.judge.domain.model.WorkerRecords.FinishCommand;
import com.algoprep.judge.domain.model.WorkerRecords.FinishKind;
import com.algoprep.judge.domain.ports.out.JudgeMaintenanceRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
final class SweepStaleSubmissionsCommandHandler implements SweepStaleSubmissionsUseCase {
  private final JudgeMaintenanceRepository repository;
  private final WorkerTransactions transactions;
  private final FinalizeSubmissionCommandHandler finalizer;
  private final int batchSize;
  private final int timeoutSeconds;

  SweepStaleSubmissionsCommandHandler(
      JudgeMaintenanceRepository repository,
      WorkerTransactions transactions,
      FinalizeSubmissionCommandHandler finalizer,
      @Value("${judge.maintenance.sweep-batch-size:200}") int batchSize,
      @Value("${judge.maintenance.timeout-seconds:300}") int timeoutSeconds) {
    if (batchSize < 1 || batchSize > 200 || timeoutSeconds < 1) {
      throw new IllegalArgumentException("Invalid sweep configuration");
    }
    this.repository = repository;
    this.transactions = transactions;
    this.finalizer = finalizer;
    this.batchSize = batchSize;
    this.timeoutSeconds = timeoutSeconds;
  }

  @Override
  public int sweep() {
    int finished = 0;
    for (var id : repository.staleCandidates(batchSize, timeoutSeconds)) {
      boolean changed =
          transactions.execute(
              () -> {
                if (!repository.claimStale(id, timeoutSeconds)) {
                  return false;
                }
                return finalizer
                    .finishLocked(
                        new FinishCommand(id, FinishKind.SYSTEM_ERROR, "WORKER_TIMEOUT", null))
                    .isPresent();
              });
      if (changed) {
        finished++;
      }
    }
    return finished;
  }
}
