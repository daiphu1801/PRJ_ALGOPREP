package com.algoprep.aireview.application.ports.in;

public interface CleanAiOutboxUseCase {
  /** JOB-AI-02. Returns the number of rows deleted (0 when another instance holds the lock). */
  int clean();
}
