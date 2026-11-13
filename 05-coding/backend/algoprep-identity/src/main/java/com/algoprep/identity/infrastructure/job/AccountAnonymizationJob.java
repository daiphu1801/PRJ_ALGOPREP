package com.algoprep.identity.infrastructure.job;

import com.algoprep.identity.application.ports.in.AnonymizeDeletedAccountsUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * JOB-ID-02, daily 03:00 server time (DD jobs section 2). Writes no audit row (no actor).
 *
 * <p>ponytail: no advisory lock, assumes a single instance; add {@code pg_try_advisory_xact_lock}
 * (DD jobs section 0) before running several instances. {@code SKIP LOCKED} already keeps two runs
 * from touching the same row.
 */
@Component
class AccountAnonymizationJob {
  private static final Logger LOG = LoggerFactory.getLogger(AccountAnonymizationJob.class);

  private final AnonymizeDeletedAccountsUseCase anonymize;

  AccountAnonymizationJob(AnonymizeDeletedAccountsUseCase anonymize) {
    this.anonymize = anonymize;
  }

  @Scheduled(cron = "0 0 3 * * *")
  void run() {
    try {
      LOG.info("JOB-ID-02 anonymised {} deleted accounts", anonymize.anonymize());
    } catch (RuntimeException e) {
      LOG.error("JOB-ID-02 anonymisation failed; retrying at the next run", e);
    }
  }
}
