package com.algoprep.problembank.infrastructure.job;

import com.algoprep.problembank.application.ports.in.CleanProblemOutboxUseCase;
import com.algoprep.problembank.application.ports.in.RelayProblemOutboxUseCase;
import java.util.concurrent.TimeUnit;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** JOB-PB-01 relay and JOB-PB-02 cleanup share one dedicated scheduler thread. */
@Component
@ConditionalOnProperty(name = "problem.outbox.enabled", havingValue = "true")
class ProblemOutboxJobs {
  private final RelayProblemOutboxUseCase relay;
  private final CleanProblemOutboxUseCase cleanup;

  ProblemOutboxJobs(RelayProblemOutboxUseCase relay, CleanProblemOutboxUseCase cleanup) {
    this.relay = relay;
    this.cleanup = cleanup;
  }

  @Scheduled(
      fixedDelayString = "${problem.outbox.relay-delay-ms:2000}",
      scheduler = "problemOutboxScheduler")
  void relayTick() {
    relay.relay();
  }

  @Scheduled(
      fixedDelayString = "${problem.outbox.cleanup-delay-hours:24}",
      initialDelay = 1,
      timeUnit = TimeUnit.HOURS,
      scheduler = "problemOutboxScheduler")
  void cleanupTick() {
    cleanup.clean();
  }
}
