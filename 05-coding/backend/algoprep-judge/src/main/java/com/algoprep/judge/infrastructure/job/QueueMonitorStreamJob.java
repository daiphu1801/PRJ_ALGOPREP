package com.algoprep.judge.infrastructure.job;

import com.algoprep.judge.application.ports.in.BroadcastQueueMonitorUseCase;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
    name = "judge.admin-stream.enabled",
    havingValue = "true",
    matchIfMissing = true)
class QueueMonitorStreamJob {
  private final BroadcastQueueMonitorUseCase broadcast;

  QueueMonitorStreamJob(BroadcastQueueMonitorUseCase broadcast) {
    this.broadcast = broadcast;
  }

  @Bean
  static ThreadPoolTaskScheduler queueMonitorStreamScheduler() {
    var scheduler = new ThreadPoolTaskScheduler();
    scheduler.setPoolSize(1);
    scheduler.setThreadNamePrefix("queue-monitor-stream-");
    scheduler.setRemoveOnCancelPolicy(true);
    return scheduler;
  }

  @Scheduled(
      scheduler = "queueMonitorStreamScheduler",
      fixedDelayString = "${judge.admin-stream.interval:5s}",
      initialDelayString = "${judge.admin-stream.interval:5s}")
  void tick() {
    broadcast.broadcast();
  }
}
