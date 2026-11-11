package com.algoprep.aireview.infrastructure.job;

import com.algoprep.aireview.application.dto.AnomalyRunOutcome;
import com.algoprep.aireview.application.ports.in.DetectAiUsageAnomaliesUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * JOB-AI-05 (DD jobs F5 section 5): every hour on the hour, UTC+7. A failure is logged and the next
 * hour retries; the job only reads token_usage and writes soft alerts.
 */
@Component
public class AiUsageAnomalyJob {
  private static final Logger LOG = LoggerFactory.getLogger(AiUsageAnomalyJob.class);

  private final DetectAiUsageAnomaliesUseCase detect;

  AiUsageAnomalyJob(DetectAiUsageAnomaliesUseCase detect) {
    this.detect = detect;
  }

  @Scheduled(cron = "0 0 * * * *", zone = "Asia/Ho_Chi_Minh")
  public void run() {
    execute();
  }

  /** Runs one detection; returns null when it failed (the failure is logged, never thrown). */
  AnomalyRunOutcome execute() {
    try {
      AnomalyRunOutcome outcome = detect.detect();
      LOG.info("JOB-AI-05 skippedByLock={} created={}", outcome.skippedByLock(), outcome.created());
      return outcome;
    } catch (RuntimeException failure) {
      LOG.error("JOB-AI-05 failed; the next hour retries", failure);
      return null;
    }
  }
}
