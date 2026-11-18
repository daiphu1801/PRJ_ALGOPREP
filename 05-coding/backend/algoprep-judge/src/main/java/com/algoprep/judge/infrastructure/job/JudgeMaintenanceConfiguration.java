package com.algoprep.judge.infrastructure.job;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "judge.maintenance.enabled", havingValue = "true")
class JudgeMaintenanceConfiguration {
  @Bean
  ThreadPoolTaskScheduler judgeMaintenanceScheduler() {
    var scheduler = new ThreadPoolTaskScheduler();
    scheduler.setThreadNamePrefix("judge-maintenance-");
    scheduler.setPoolSize(3);
    scheduler.setRemoveOnCancelPolicy(true);
    return scheduler;
  }
}
