package com.algoprep.judge.infrastructure.job;

import com.algoprep.judge.application.ports.in.RelayJudgeOutboxUseCase;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** JOB-JD-02 is independent of worker pause and grading capacity. */
@Component
@ConditionalOnProperty(name = "judge.outbox.enabled", havingValue = "true")
class JudgeOutboxRelayJob {
  private final RelayJudgeOutboxUseCase relay;

  JudgeOutboxRelayJob(RelayJudgeOutboxUseCase relay) {
    this.relay = relay;
  }

  @Scheduled(
      fixedDelayString = "${judge.outbox.fixed-delay-ms:2000}",
      scheduler = "judgeOutboxScheduler")
  void tick() {
    relay.relay();
  }
}
