package com.algoprep.judge.application.command;

import com.algoprep.judge.application.ports.in.PurgeTestcaseDetailsUseCase;
import com.algoprep.judge.domain.ports.out.JudgeMaintenanceRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
class PurgeTestcaseDetailsCommandHandler implements PurgeTestcaseDetailsUseCase {
  private final JudgeMaintenanceRepository repository;
  private final WorkerTransactions transactions;
  private final int days;
  private final int batchSize;

  PurgeTestcaseDetailsCommandHandler(
      JudgeMaintenanceRepository repository,
      WorkerTransactions transactions,
      @Value("${judge.maintenance.retention-days:90}") int days,
      @Value("${judge.maintenance.cleanup-batch-size:5000}") int batchSize) {
    JudgeRetention.validate(days, batchSize);
    this.repository = repository;
    this.transactions = transactions;
    this.days = days;
    this.batchSize = batchSize;
  }

  @Override
  public long purge() {
    return JudgeRetention.purge(
        repository,
        transactions,
        "JOB-JD-04",
        batchSize,
        () -> repository.deleteTestcaseDetails(days, batchSize));
  }
}
