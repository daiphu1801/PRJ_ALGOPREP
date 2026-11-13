package com.algoprep.judge.application.command;

import com.algoprep.judge.domain.ports.out.JudgeMaintenanceRepository;
import java.util.function.IntSupplier;

final class JudgeRetention {
  private JudgeRetention() {}

  static long purge(
      JudgeMaintenanceRepository repository,
      WorkerTransactions transactions,
      String job,
      int batchSize,
      IntSupplier delete) {
    long total = 0;
    int count;
    do {
      count = transactions.execute(() -> repository.tryJobLock(job) ? delete.getAsInt() : 0);
      total += count;
    } while (count == batchSize);
    return total;
  }

  static void validate(int days, int batchSize) {
    if (days < 1 || batchSize < 1 || batchSize > 5000) {
      throw new IllegalArgumentException("Invalid retention configuration");
    }
  }
}
