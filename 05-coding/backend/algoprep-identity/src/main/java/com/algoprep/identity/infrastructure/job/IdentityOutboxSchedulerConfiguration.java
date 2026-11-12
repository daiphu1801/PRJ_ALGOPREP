package com.algoprep.identity.infrastructure.job;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@Configuration
@ConditionalOnProperty(name = "identity.outbox.enabled", havingValue = "true")
class IdentityOutboxSchedulerConfiguration {
  @Bean
  ThreadPoolTaskScheduler identityOutboxScheduler() {
    var scheduler = new ThreadPoolTaskScheduler();
    scheduler.setThreadNamePrefix("identity-outbox-");
    scheduler.setPoolSize(1);
    scheduler.setRemoveOnCancelPolicy(true);
    return scheduler;
  }
}
