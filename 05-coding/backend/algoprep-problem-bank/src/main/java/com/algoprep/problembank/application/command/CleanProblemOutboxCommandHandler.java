package com.algoprep.problembank.application.command;

import com.algoprep.problembank.application.ports.in.CleanProblemOutboxUseCase;
import com.algoprep.problembank.domain.ports.out.ProblemOutboxRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
class CleanProblemOutboxCommandHandler implements CleanProblemOutboxUseCase {
  private static final Logger LOG = LoggerFactory.getLogger(CleanProblemOutboxCommandHandler.class);
  private static final int CHUNK = 1000;
  private static final int MAX_CHUNKS = 100;
  private final ProblemOutboxRepository repository;
  private final int retentionDays;

  CleanProblemOutboxCommandHandler(
      ProblemOutboxRepository repository,
      @Value("${problem.outbox.retention-days:90}") int retentionDays) {
    this.repository = repository;
    this.retentionDays = retentionDays;
  }

  /** Each repository call is its own transaction, so a failure rolls back only that chunk. */
  @Override
  public int clean() {
    int total = 0;
    try {
      for (int i = 0; i < MAX_CHUNKS; i++) {
        int deleted = repository.deletePublishedOlderThanDays(retentionDays, CHUNK);
        total += deleted;
        if (deleted < CHUNK) {
          break;
        }
      }
    } catch (RuntimeException failure) {
      LOG.error(
          "Problem outbox cleanup failed, retry next run: {}", failure.getClass().getSimpleName());
    }
    return total;
  }
}
