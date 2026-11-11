package com.algoprep.aireview.infrastructure.job;

import com.algoprep.aireview.application.dto.SweepOutcome;
import com.algoprep.aireview.application.ports.in.SweepIdleInterviewSessionsUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * JOB-AI-04 (DD jobs F5 section 4): every 60 seconds, on its own single-thread scheduler because a
 * scoring call can take up to 30 seconds. Off unless {@code ai.interview.sweep.enabled=true}. A
 * failure is logged and the next run retries; nothing reaches the other contexts.
 */
@Component
@ConditionalOnProperty(name = "ai.interview.sweep.enabled", havingValue = "true")
public class AiInterviewSweepJob {
  private static final Logger LOG = LoggerFactory.getLogger(AiInterviewSweepJob.class);

  private final SweepIdleInterviewSessionsUseCase sweep;

  AiInterviewSweepJob(SweepIdleInterviewSessionsUseCase sweep) {
    this.sweep = sweep;
  }

  @Scheduled(
      fixedDelayString = "${ai.interview.sweep.delay-ms:60000}",
      scheduler = "aiInterviewSweepScheduler")
  public void run() {
    try {
      SweepOutcome outcome = sweep.sweep();
      LOG.info(
          "JOB-AI-04 scored={} abandoned={} skipped={} failed={}",
          outcome.scored(),
          outcome.abandoned(),
          outcome.skipped(),
          outcome.failed());
    } catch (RuntimeException failure) {
      LOG.error("JOB-AI-04 failed; the next run retries", failure);
    }
  }
}
