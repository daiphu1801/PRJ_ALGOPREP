package com.algoprep.judge.infrastructure.job;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.algoprep.judge.application.ports.in.PurgePublishedJudgeEventsUseCase;
import com.algoprep.judge.application.ports.in.PurgeTestcaseDetailsUseCase;
import com.algoprep.judge.application.ports.in.SweepStaleSubmissionsUseCase;
import org.junit.jupiter.api.Test;

class JudgeMaintenanceJobsTest {
  @org.springframework.context.annotation.Configuration(proxyBeanMethods = false)
  @org.springframework.scheduling.annotation.EnableScheduling
  static class Scheduling {}

  @Test
  void enabledMaintenanceInvokesSweepOnItsOwnScheduler() throws Exception {
    var called = new java.util.concurrent.CountDownLatch(1);
    var threadName = new java.util.concurrent.atomic.AtomicReference<String>();
    try (var context =
        new org.springframework.context.annotation.AnnotationConfigApplicationContext()) {
      context
          .getEnvironment()
          .getPropertySources()
          .addFirst(
              new org.springframework.core.env.MapPropertySource(
                  "maintenance-test", java.util.Map.of("judge.maintenance.enabled", "true")));
      context.registerBean(
          SweepStaleSubmissionsUseCase.class,
          () ->
              () -> {
                threadName.set(Thread.currentThread().getName());
                called.countDown();
                return 0;
              });
      context.registerBean(PurgePublishedJudgeEventsUseCase.class, () -> () -> 0);
      context.registerBean(PurgeTestcaseDetailsUseCase.class, () -> () -> 0);
      context.register(
          Scheduling.class, JudgeMaintenanceJobs.class, JudgeMaintenanceConfiguration.class);
      context.refresh();
      assertThat(called.await(5, java.util.concurrent.TimeUnit.SECONDS)).isTrue();
      assertThat(threadName.get()).startsWith("judge-maintenance-");
    }
  }

  @Test
  void jobsDelegateAndFailuresDoNotEscapeTheScheduledInvocation() {
    var sweep = mock(SweepStaleSubmissionsUseCase.class);
    var outbox = mock(PurgePublishedJudgeEventsUseCase.class);
    var details = mock(PurgeTestcaseDetailsUseCase.class);
    var jobs = new JudgeMaintenanceJobs(sweep, outbox, details);
    jobs.sweep();
    jobs.purgeOutbox();
    jobs.purgeDetails();
    verify(sweep).sweep();
    verify(outbox).purge();
    verify(details).purge();
    when(sweep.sweep()).thenThrow(new IllegalStateException("private SQL"));
    assertThatCode(jobs::sweep).doesNotThrowAnyException();
  }

  @Test
  void maintenanceHasDedicatedCapacityForAllThreeJobs() {
    var scheduler = new JudgeMaintenanceConfiguration().judgeMaintenanceScheduler();
    try {
      assertThat(scheduler.getThreadNamePrefix()).isEqualTo("judge-maintenance-");
      assertThat(scheduler.getPoolSize()).isEqualTo(3);
    } finally {
      scheduler.destroy();
    }
  }
}
