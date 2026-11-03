package com.algoprep.judge.infrastructure.job;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/** Broker waits must not delay worker pause, autoscale, or other application jobs. */
@Configuration
@ConditionalOnProperty(name = "judge.outbox.enabled", havingValue = "true")
class JudgeOutboxRelayConfiguration {
  @Bean
  ThreadPoolTaskScheduler judgeOutboxScheduler() {
    var scheduler = new ThreadPoolTaskScheduler();
    scheduler.setThreadNamePrefix("judge-outbox-");
    scheduler.setPoolSize(1);
    scheduler.setRemoveOnCancelPolicy(true);
    return scheduler;
  }
}
