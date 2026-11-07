package com.algoprep.problembank.infrastructure.job;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@Configuration
@ConditionalOnProperty(name = "problem.outbox.enabled", havingValue = "true")
class ProblemOutboxSchedulerConfiguration {
  @Bean
  ThreadPoolTaskScheduler problemOutboxScheduler() {
    var scheduler = new ThreadPoolTaskScheduler();
    scheduler.setThreadNamePrefix("problem-outbox-");
    scheduler.setPoolSize(1);
    scheduler.setRemoveOnCancelPolicy(true);
    return scheduler;
  }
}
