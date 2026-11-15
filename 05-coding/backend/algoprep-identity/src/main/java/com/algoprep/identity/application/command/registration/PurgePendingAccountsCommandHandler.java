package com.algoprep.identity.application.command;

import com.algoprep.identity.application.ports.in.PurgePendingAccountsUseCase;
import com.algoprep.identity.domain.ports.out.AccountLifecycleRepository;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * JOB-ID-06. Not {@code @Transactional}: every batch is one statement and commits on its own.
 *
 * <p>Only self-registered accounts go ({@code must_change_password = false}); an account an admin
 * created also waits in {@code PENDING_VERIFICATION} but carries a temporary password and must
 * stay.
 */
@Service
public class PurgePendingAccountsCommandHandler implements PurgePendingAccountsUseCase {
  static final int BATCH_SIZE = 500;

  private final AccountLifecycleRepository accounts;
  private final Duration ttl;

  public PurgePendingAccountsCommandHandler(
      AccountLifecycleRepository accounts,
      @Value("${algoprep.identity.pending-account-ttl:7d}") Duration ttl) {
    this.accounts = accounts;
    this.ttl = ttl;
  }

  @Override
  public int purge() {
    int total = 0;
    int deleted;
    do {
      deleted = accounts.deleteStalePendingBatch(ttl, BATCH_SIZE);
      total += deleted;
    } while (deleted == BATCH_SIZE);
    return total;
  }
}
