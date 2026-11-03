package com.algoprep.identity.application.command;

import com.algoprep.identity.application.ports.in.PurgeExpiredAuditLogsUseCase;
import com.algoprep.identity.domain.model.ExpiredLogPolicy;
import com.algoprep.identity.domain.model.LogRetentionSettings;
import com.algoprep.identity.domain.ports.out.LogRetentionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * JOB-ID-05 / BR-20. Not {@code @Transactional}: each batch is one atomic statement, so a failing
 * batch never undoes the ones already done. At most {@value #MAX_BATCHES} batches of {@value
 * #BATCH_SIZE} per run; what is left waits for the next run. The run writes no audit row (no actor,
 * DD jobs section 0).
 */
@Service
class PurgeExpiredAuditLogsCommandHandler implements PurgeExpiredAuditLogsUseCase {
  private static final Logger LOG =
      LoggerFactory.getLogger(PurgeExpiredAuditLogsCommandHandler.class);
  static final int BATCH_SIZE = 5_000;
  static final int MAX_BATCHES = 20;

  private final LogRetentionRepository retention;

  PurgeExpiredAuditLogsCommandHandler(LogRetentionRepository retention) {
    this.retention = retention;
  }

  @Override
  public int purge() {
    LogRetentionSettings settings = retention.read();
    if (settings.expiredPolicy() != ExpiredLogPolicy.DELETE) {
      LOG.warn(
          "JOB-ID-05 policy {} is not supported yet; keeping all rows", settings.expiredPolicy());
      return 0;
    }
    int total = 0;
    for (int batch = 0; batch < MAX_BATCHES; batch++) {
      int done = retention.purgeExpiredBatch(settings.retentionDays(), BATCH_SIZE);
      total += done;
      if (done < BATCH_SIZE) {
        break;
      }
    }
    return total;
  }
}
