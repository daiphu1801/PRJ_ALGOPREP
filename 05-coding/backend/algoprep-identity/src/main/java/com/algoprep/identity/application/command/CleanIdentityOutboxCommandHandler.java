package com.algoprep.identity.application.command;

import com.algoprep.identity.application.ports.in.CleanIdentityOutboxUseCase;
import com.algoprep.identity.domain.ports.out.IdentityOutboxRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
class CleanIdentityOutboxCommandHandler implements CleanIdentityOutboxUseCase {
  private static final Logger LOG =
      LoggerFactory.getLogger(CleanIdentityOutboxCommandHandler.class);
  private static final int CHUNK = 1000;
  private static final int MAX_CHUNKS = 100;
  private final IdentityOutboxRepository repository;
  private final int retentionDays;

  CleanIdentityOutboxCommandHandler(
      IdentityOutboxRepository repository,
      @Value("${identity.outbox.retention-days:90}") int retentionDays) {
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
          "Identity outbox cleanup failed, retry next run: {}", failure.getClass().getSimpleName());
    }
    return total;
  }
}
