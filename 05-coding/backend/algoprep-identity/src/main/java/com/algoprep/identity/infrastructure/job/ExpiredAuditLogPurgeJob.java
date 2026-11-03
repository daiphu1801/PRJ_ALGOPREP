package com.algoprep.identity.infrastructure.job;

import com.algoprep.identity.application.ports.in.PurgeExpiredAuditLogsUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * JOB-ID-05, daily 04:30 server time (DD jobs section 5). Writes no audit row (no actor).
 *
 * <p>ponytail: no advisory lock, assumes a single instance; add {@code pg_try_advisory_xact_lock}
 * (DD jobs section 0) before running several instances. {@code SKIP LOCKED} already keeps two runs
 * from deleting the same row.
 */
@Component
class ExpiredAuditLogPurgeJob {
  private static final Logger LOG = LoggerFactory.getLogger(ExpiredAuditLogPurgeJob.class);

  private final PurgeExpiredAuditLogsUseCase purge;

  ExpiredAuditLogPurgeJob(PurgeExpiredAuditLogsUseCase purge) {
    this.purge = purge;
  }

  @Scheduled(cron = "0 30 4 * * *")
  void run() {
    try {
      LOG.info("JOB-ID-05 purged {} expired audit rows", purge.purge());
    } catch (RuntimeException e) {
      LOG.error("JOB-ID-05 purge failed; retrying at the next run", e);
    }
  }
}
