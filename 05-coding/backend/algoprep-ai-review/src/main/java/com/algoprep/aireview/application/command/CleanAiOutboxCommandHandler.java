package com.algoprep.aireview.application.command;

import com.algoprep.aireview.application.ports.in.CleanAiOutboxUseCase;
import com.algoprep.aireview.domain.ports.out.AiOutboxRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * JOB-AI-02: deletes rows published more than {@code ai.outbox.retention-days} ago, in chunks, and
 * never a row that is still pending. One transaction holds the advisory lock for the whole run, so
 * a second instance gets 0 instead of racing; a failure rolls the run back and the next day
 * retries.
 */
@Service
class CleanAiOutboxCommandHandler implements CleanAiOutboxUseCase {
  private static final int MAX_CHUNKS = 200;

  private final AiOutboxRepository repository;
  private final int retentionDays;
  private final int chunk;

  CleanAiOutboxCommandHandler(
      AiOutboxRepository repository,
      @Value("${ai.outbox.retention-days:90}") int retentionDays,
      @Value("${ai.outbox.cleanup-batch-size:5000}") int chunk) {
    this.repository = repository;
    this.retentionDays = retentionDays;
    this.chunk = chunk;
  }

  @Override
  @Transactional
  public int clean() {
    if (!repository.tryCleanupLock()) {
      return 0;
    }
    int total = 0;
    for (int i = 0; i < MAX_CHUNKS; i++) {
      int deleted = repository.deletePublishedOlderThanDays(retentionDays, chunk);
      total += deleted;
      if (deleted < chunk) {
        break;
      }
    }
    return total;
  }
}
