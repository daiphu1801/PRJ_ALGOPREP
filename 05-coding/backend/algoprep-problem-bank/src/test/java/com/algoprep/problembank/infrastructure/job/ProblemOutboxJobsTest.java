package com.algoprep.problembank.infrastructure.job;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.algoprep.problembank.application.ports.in.CleanProblemOutboxUseCase;
import com.algoprep.problembank.application.ports.in.RelayProblemOutboxUseCase;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.MapPropertySource;
import org.springframework.scheduling.annotation.EnableScheduling;

class ProblemOutboxJobsTest {
  @Configuration(proxyBeanMethods = false)
  @EnableScheduling
  static class Scheduling {}

  @Test
  void ticksDelegateToTheUseCases() {
    var relay = mock(RelayProblemOutboxUseCase.class);
    var cleanup = mock(CleanProblemOutboxUseCase.class);
    var jobs = new ProblemOutboxJobs(relay, cleanup);
    jobs.relayTick();
    jobs.cleanupTick();
    verify(relay).relay();
    verify(cleanup).clean();
  }

  @Test
  void enablingTheRelayRunsItOnItsDedicatedScheduler() throws Exception {
    var called = new CountDownLatch(1);
    var threadName = new AtomicReference<String>();
    try (var context = new AnnotationConfigApplicationContext()) {
      context
          .getEnvironment()
          .getPropertySources()
          .addFirst(
              new MapPropertySource(
                  "relay-test",
                  Map.of("problem.outbox.enabled", "true", "problem.outbox.relay-delay-ms", "50")));
      context.registerBean(
          RelayProblemOutboxUseCase.class,
          () ->
              () -> {
                threadName.set(Thread.currentThread().getName());
                called.countDown();
                return 0;
              });
      context.registerBean(CleanProblemOutboxUseCase.class, () -> () -> 0);
      context.register(
          Scheduling.class, ProblemOutboxJobs.class, ProblemOutboxSchedulerConfiguration.class);
      context.refresh();
      assertThat(called.await(5, TimeUnit.SECONDS)).isTrue();
      assertThat(threadName.get()).startsWith("problem-outbox-");
    }
  }

  @Test
  void schedulerIsASingleDedicatedThread() {
    var scheduler = new ProblemOutboxSchedulerConfiguration().problemOutboxScheduler();
    try {
      assertThat(scheduler.getThreadNamePrefix()).isEqualTo("problem-outbox-");
      assertThat(scheduler.getPoolSize()).isEqualTo(1);
    } finally {
      scheduler.destroy();
    }
  }
}
