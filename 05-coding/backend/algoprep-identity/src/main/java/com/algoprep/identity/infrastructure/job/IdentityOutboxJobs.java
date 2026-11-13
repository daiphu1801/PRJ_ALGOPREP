package com.algoprep.identity.infrastructure.job;

import com.algoprep.identity.application.ports.in.CleanIdentityOutboxUseCase;
import com.algoprep.identity.application.ports.in.RelayIdentityOutboxUseCase;
import java.util.concurrent.TimeUnit;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** JOB-ID-OUTBOX-01 relay and JOB-ID-OUTBOX-02 cleanup share one dedicated scheduler thread. */
@Component
@ConditionalOnProperty(name = "identity.outbox.enabled", havingValue = "true")
class IdentityOutboxJobs {
  private final RelayIdentityOutboxUseCase relay;
  private final CleanIdentityOutboxUseCase cleanup;

  IdentityOutboxJobs(RelayIdentityOutboxUseCase relay, CleanIdentityOutboxUseCase cleanup) {
    this.relay = relay;
    this.cleanup = cleanup;
  }

  @Scheduled(
      fixedDelayString = "${identity.outbox.relay-delay-ms:2000}",
      scheduler = "identityOutboxScheduler")
  void relayTick() {
    relay.relay();
  }

  @Scheduled(
      fixedDelayString = "${identity.outbox.cleanup-delay-hours:24}",
      initialDelay = 1,
      timeUnit = TimeUnit.HOURS,
      scheduler = "identityOutboxScheduler")
  void cleanupTick() {
    cleanup.clean();
  }
}
