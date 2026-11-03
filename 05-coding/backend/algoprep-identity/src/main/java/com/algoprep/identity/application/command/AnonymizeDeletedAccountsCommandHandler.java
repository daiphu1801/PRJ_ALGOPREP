package com.algoprep.identity.application.command;

import com.algoprep.identity.application.ports.in.AnonymizeDeletedAccountsUseCase;
import com.algoprep.identity.domain.ports.out.AccountLifecycleRepository;
import org.springframework.stereotype.Service;

/**
 * JOB-ID-02. Not {@code @Transactional}: each batch is one atomic statement, so a failing batch
 * never undoes the ones already done. At most five batches of 200 per run (DD jobs section 2); what
 * is left waits for the next run.
 */
@Service
public class AnonymizeDeletedAccountsCommandHandler implements AnonymizeDeletedAccountsUseCase {
  static final int BATCH_SIZE = 200;
  static final int MAX_BATCHES = 5;

  private final AccountLifecycleRepository accounts;

  public AnonymizeDeletedAccountsCommandHandler(AccountLifecycleRepository accounts) {
    this.accounts = accounts;
  }

  @Override
  public int anonymize() {
    int total = 0;
    for (int batch = 0; batch < MAX_BATCHES; batch++) {
      int done = accounts.anonymizeDueBatch(BATCH_SIZE);
      total += done;
      if (done < BATCH_SIZE) {
        break;
      }
    }
    return total;
  }
}
