package com.algoprep.identity.infrastructure.job;

import com.algoprep.identity.application.ports.in.PurgePendingAccountsUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * JOB-ID-06, daily 04:30 server time (DD jobs section 4). Writes no audit row (no actor).
 *
 * <p>ponytail: no advisory lock, assumes a single instance; add {@code pg_try_advisory_xact_lock}
 * (DD jobs section 0) before running several instances.
 */
@Component
class PendingAccountPurgeJob {
  private static final Logger LOG = LoggerFactory.getLogger(PendingAccountPurgeJob.class);

  private final PurgePendingAccountsUseCase purge;

  PendingAccountPurgeJob(PurgePendingAccountsUseCase purge) {
    this.purge = purge;
  }

  @Scheduled(cron = "0 30 4 * * *")
  void run() {
    try {
      LOG.info("JOB-ID-06 purged {} pending accounts", purge.purge());
    } catch (RuntimeException e) {
      LOG.error("JOB-ID-06 pending account purge failed; retrying at the next run", e);
    }
  }
}
