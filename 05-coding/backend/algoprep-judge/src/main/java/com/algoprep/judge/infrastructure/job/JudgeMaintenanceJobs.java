package com.algoprep.judge.infrastructure.job;

import com.algoprep.judge.application.ports.in.PurgePublishedJudgeEventsUseCase;
import com.algoprep.judge.application.ports.in.PurgeTestcaseDetailsUseCase;
import com.algoprep.judge.application.ports.in.SweepStaleSubmissionsUseCase;
import java.util.function.LongSupplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "judge.maintenance.enabled", havingValue = "true")
class JudgeMaintenanceJobs {
  private static final Logger LOG = LoggerFactory.getLogger(JudgeMaintenanceJobs.class);
  private final SweepStaleSubmissionsUseCase sweep;
  private final PurgePublishedJudgeEventsUseCase outbox;
  private final PurgeTestcaseDetailsUseCase details;

  JudgeMaintenanceJobs(
      SweepStaleSubmissionsUseCase sweep,
      PurgePublishedJudgeEventsUseCase outbox,
      PurgeTestcaseDetailsUseCase details) {
    this.sweep = sweep;
    this.outbox = outbox;
    this.details = details;
  }

  @Scheduled(
      fixedDelayString = "${judge.maintenance.sweep-delay-ms:60000}",
      scheduler = "judgeMaintenanceScheduler")
  void sweep() {
    run("JOB-JD-01", sweep::sweep);
  }

  @Scheduled(
      cron = "${judge.maintenance.outbox-cron:0 0 3 * * *}",
      zone = "Asia/Ho_Chi_Minh",
      scheduler = "judgeMaintenanceScheduler")
  void purgeOutbox() {
    run("JOB-JD-03", outbox::purge);
  }

  @Scheduled(
      cron = "${judge.maintenance.details-cron:0 30 3 * * *}",
      zone = "Asia/Ho_Chi_Minh",
      scheduler = "judgeMaintenanceScheduler")
  void purgeDetails() {
    run("JOB-JD-04", details::purge);
  }

  private void run(String job, LongSupplier operation) {
    try {
      LOG.info("{} affected {} rows", job, operation.getAsLong());
    } catch (RuntimeException unavailable) {
      LOG.error("{} failed; retrying at the next scheduled run", job);
    }
  }
}
