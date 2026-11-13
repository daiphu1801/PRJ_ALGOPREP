package com.algoprep.judge.infrastructure.job;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.algoprep.judge.application.ports.in.RelayJudgeOutboxUseCase;
import org.junit.jupiter.api.Test;

class JudgeOutboxRelayJobTest {
  @org.springframework.context.annotation.Configuration(proxyBeanMethods = false)
  @org.springframework.scheduling.annotation.EnableScheduling
  static class Scheduling {}

  @Test
  void enablingTheRelayRunsItOnItsDedicatedScheduler() throws Exception {
    var called = new java.util.concurrent.CountDownLatch(1);
    var threadName = new java.util.concurrent.atomic.AtomicReference<String>();
    try (var context =
        new org.springframework.context.annotation.AnnotationConfigApplicationContext()) {
      context
          .getEnvironment()
          .getPropertySources()
          .addFirst(
              new org.springframework.core.env.MapPropertySource(
                  "relay-test",
                  java.util.Map.of(
                      "judge.outbox.enabled", "true", "judge.outbox.fixed-delay-ms", "2000")));
      context.registerBean(
          RelayJudgeOutboxUseCase.class,
          () ->
              () -> {
                threadName.set(Thread.currentThread().getName());
                called.countDown();
                return 0;
              });
      context.register(
          Scheduling.class, JudgeOutboxRelayJob.class, JudgeOutboxRelayConfiguration.class);
      context.refresh();
      assertThat(called.await(5, java.util.concurrent.TimeUnit.SECONDS)).isTrue();
      assertThat(threadName.get()).startsWith("judge-outbox-");
    }
  }

  @Test
  void tickDelegatesToTheTransactionalUseCase() {
    var relay = mock(RelayJudgeOutboxUseCase.class);
    new JudgeOutboxRelayJob(relay).tick();
    verify(relay).relay();
  }

  @Test
  void dedicatedSchedulerDoesNotOccupyWorkerControlThreads() {
    var scheduler = new JudgeOutboxRelayConfiguration().judgeOutboxScheduler();
    try {
      assertThat(scheduler.getThreadNamePrefix()).isEqualTo("judge-outbox-");
      assertThat(scheduler.getPoolSize()).isEqualTo(1);
    } finally {
      scheduler.destroy();
    }
  }
}
