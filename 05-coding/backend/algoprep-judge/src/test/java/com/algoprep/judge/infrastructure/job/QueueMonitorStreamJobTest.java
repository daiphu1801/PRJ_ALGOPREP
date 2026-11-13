package com.algoprep.judge.infrastructure.job;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.algoprep.judge.application.ports.in.BroadcastQueueMonitorUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.annotation.Scheduled;

class QueueMonitorStreamJobTest {
  @Test
  void defaultFiveSecondPollUsesADedicatedSerialScheduler() throws Exception {
    var broadcast = mock(BroadcastQueueMonitorUseCase.class);
    new QueueMonitorStreamJob(broadcast).tick();
    verify(broadcast).broadcast();
    var scheduled =
        QueueMonitorStreamJob.class.getDeclaredMethod("tick").getAnnotation(Scheduled.class);
    assertThat(scheduled.fixedDelayString()).isEqualTo("${judge.admin-stream.interval:5s}");
    assertThat(scheduled.initialDelayString()).isEqualTo("${judge.admin-stream.interval:5s}");
    assertThat(scheduled.scheduler()).isEqualTo("queueMonitorStreamScheduler");
    assertThat(QueueMonitorStreamJob.queueMonitorStreamScheduler().getPoolSize()).isEqualTo(1);
  }
}
