package com.algoprep.identity.application.command;

import com.algoprep.identity.application.ports.in.PurgeRefreshTokensUseCase;
import com.algoprep.identity.domain.ports.out.RefreshTokenRepository;
import org.springframework.stereotype.Service;

/**
 * JOB-ID-04. Not {@code @Transactional}: every batch is one DELETE statement and commits on its
 * own, so a failing batch never undoes the ones already done.
 */
@Service
public class PurgeRefreshTokensCommandHandler implements PurgeRefreshTokensUseCase {
  static final int BATCH_SIZE = 5000;

  private final RefreshTokenRepository tokens;

  public PurgeRefreshTokensCommandHandler(RefreshTokenRepository tokens) {
    this.tokens = tokens;
  }

  @Override
  public int purge() {
    int total = 0;
    int deleted;
    do {
      deleted = tokens.deleteExpiredBatch(BATCH_SIZE);
      total += deleted;
    } while (deleted == BATCH_SIZE);
    return total;
  }
}
