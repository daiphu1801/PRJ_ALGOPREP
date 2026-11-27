package com.algoprep.aireview.infrastructure.job;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@Configuration
@ConditionalOnProperty(name = "ai.outbox.enabled", havingValue = "true")
class AiOutboxSchedulerConfiguration {
  @Bean
  ThreadPoolTaskScheduler aiOutboxScheduler() {
    var scheduler = new ThreadPoolTaskScheduler();
    scheduler.setThreadNamePrefix("ai-outbox-");
    scheduler.setPoolSize(1);
    scheduler.setRemoveOnCancelPolicy(true);
    return scheduler;
  }
}
