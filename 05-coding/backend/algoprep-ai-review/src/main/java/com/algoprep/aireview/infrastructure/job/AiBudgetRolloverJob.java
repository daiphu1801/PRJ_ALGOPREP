package com.algoprep.aireview.infrastructure.job;

import com.algoprep.aireview.application.dto.RolloverOutcome;
import com.algoprep.aireview.application.ports.in.RolloverAiBudgetPeriodUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * JOB-AI-03 (DD jobs F5 section 3): 00:05 on the 1st of every month, UTC+7, and once at startup so
 * a month boundary that passed while the system was down is still handled. Logs one INFO line and
 * writes no audit row. A failure is logged and healed by the next run (the update is conditional).
 */
@Component
public class AiBudgetRolloverJob {
  private static final Logger LOG = LoggerFactory.getLogger(AiBudgetRolloverJob.class);

  private final RolloverAiBudgetPeriodUseCase rollover;

  AiBudgetRolloverJob(RolloverAiBudgetPeriodUseCase rollover) {
    this.rollover = rollover;
  }

  @Scheduled(cron = "0 5 0 1 * *", zone = "Asia/Ho_Chi_Minh")
  public void run() {
    execute();
  }

  @EventListener(ApplicationReadyEvent.class)
  void onStartup() {
    execute();
  }

  /** Runs one rollover; returns null when it failed (the failure is logged, never thrown). */
  RolloverOutcome execute() {
    try {
      RolloverOutcome outcome = rollover.rollover();
      LOG.info(
          "JOB-AI-03 skippedByLock={} changed={} periodStart={} unlocked={}",
          outcome.skippedByLock(),
          outcome.changed(),
          outcome.periodStart(),
          outcome.unlocked());
      return outcome;
    } catch (RuntimeException failure) {
      LOG.error("JOB-AI-03 failed; the next run retries", failure);
      return null;
    }
  }
}
