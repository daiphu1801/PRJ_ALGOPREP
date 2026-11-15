package com.algoprep.judge.application.command;

import com.algoprep.judge.domain.exception.ProblemGradingReadException;
import java.util.function.Supplier;

final class WorkerRetry {
  private WorkerRetry() {}

  static <T> T read(Supplier<T> operation) {
    for (int attempt = 0; ; attempt++) {
      try {
        return operation.get();
      } catch (ProblemGradingReadException e) {
        if (e.reason() == ProblemGradingReadException.Reason.SPEC_UNAVAILABLE || attempt == 2) {
          throw e;
        }
        pause(attempt);
      }
    }
  }

  static void pause(int attempt) {
    try {
      Thread.sleep(attempt == 0 ? 500 : 1000);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new ProblemGradingReadException(ProblemGradingReadException.Reason.SANDBOX_FAULT);
    }
  }
}
