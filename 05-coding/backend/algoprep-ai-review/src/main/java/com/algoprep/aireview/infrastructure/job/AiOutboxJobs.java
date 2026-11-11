package com.algoprep.aireview.infrastructure.job;

import com.algoprep.aireview.application.ports.in.CleanAiOutboxUseCase;
import com.algoprep.aireview.application.ports.in.RelayAiOutboxUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * JOB-AI-01 relay (fixedDelay 2 s) and JOB-AI-02 cleanup (03:00 UTC+7) share one scheduler thread.
 * A failure is logged and the next run retries; nothing is thrown to the scheduler, so the AI path
 * can never disturb the other contexts.
 */
@Component
@ConditionalOnProperty(name = "ai.outbox.enabled", havingValue = "true")
class AiOutboxJobs {
  private static final Logger LOG = LoggerFactory.getLogger(AiOutboxJobs.class);

  private final RelayAiOutboxUseCase relay;
  private final CleanAiOutboxUseCase cleanup;

  AiOutboxJobs(RelayAiOutboxUseCase relay, CleanAiOutboxUseCase cleanup) {
    this.relay = relay;
    this.cleanup = cleanup;
  }

  @Scheduled(fixedDelayString = "${ai.outbox.relay-delay-ms:2000}", scheduler = "aiOutboxScheduler")
  void relayTick() {
    try {
      relay.relay();
    } catch (RuntimeException failure) {
      LOG.error("JOB-AI-01 failed; the next tick retries", failure);
    }
  }

  @Scheduled(cron = "0 0 3 * * *", zone = "Asia/Ho_Chi_Minh", scheduler = "aiOutboxScheduler")
  void cleanupTick() {
    try {
      LOG.info("JOB-AI-02 deleted={}", cleanup.clean());
    } catch (RuntimeException failure) {
      LOG.error("JOB-AI-02 failed; the next run retries", failure);
    }
  }
}
