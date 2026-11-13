package com.algoprep.identity.infrastructure.job;

import com.algoprep.identity.application.ports.in.PurgeRefreshTokensUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * JOB-ID-04, daily 04:00 server time (DD jobs section 4). Writes no audit row (no actor).
 *
 * <p>ponytail: no advisory lock, assumes a single instance; add {@code pg_try_advisory_xact_lock}
 * (DD jobs section 0) before running several instances.
 */
@Component
class RefreshTokenPurgeJob {
  private static final Logger LOG = LoggerFactory.getLogger(RefreshTokenPurgeJob.class);

  private final PurgeRefreshTokensUseCase purge;

  RefreshTokenPurgeJob(PurgeRefreshTokensUseCase purge) {
    this.purge = purge;
  }

  @Scheduled(cron = "0 0 4 * * *")
  void run() {
    try {
      LOG.info("JOB-ID-04 purged {} refresh tokens", purge.purge());
    } catch (RuntimeException e) {
      LOG.error("JOB-ID-04 refresh token purge failed; retrying at the next run", e);
    }
  }
}
