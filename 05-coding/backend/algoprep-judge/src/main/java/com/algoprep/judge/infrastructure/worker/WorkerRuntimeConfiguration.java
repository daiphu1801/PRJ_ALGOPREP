package com.algoprep.judge.infrastructure.worker;

import com.algoprep.judge.application.ports.in.ProcessQueuedSubmissionUseCase;
import com.algoprep.judge.domain.ports.out.WorkerJobRepository;
import com.algoprep.judge.domain.ports.out.WorkerLeasePort;
import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "judge.worker.enabled", havingValue = "true")
@org.springframework.context.annotation.PropertySource("classpath:judge-worker.properties")
class WorkerRuntimeConfiguration {
  @Bean
  io.micrometer.core.instrument.binder.MeterBinder gradingWorkerConsumerMetrics(
      WorkerConsumptionControl control) {
    return new WorkerConsumerMetrics(control);
  }

  @Bean
  Queue gradingSubmissionQueue() {
    return new Queue("judge.submissions", true);
  }

  private SimpleMessageListenerContainer container(
      ConnectionFactory connection, ProcessQueuedSubmissionUseCase process) {
    var container = new SimpleMessageListenerContainer(connection);
    container.setQueueNames("judge.submissions");
    container.setAcknowledgeMode(AcknowledgeMode.MANUAL);
    container.setPrefetchCount(1);
    container.setConcurrency("1");
    container.setAutoStartup(false);
    container.setShutdownTimeout(0);
    container.setForceCloseChannel(false);
    container.setMessageListener(new WorkerMessageListener(process));
    return container;
  }

  @Bean
  WorkerConsumptionControl gradingConsumptionControl(
      WorkerJobRepository jobs,
      WorkerLeasePort leases,
      ConnectionFactory connection,
      ProcessQueuedSubmissionUseCase process,
      @Value("${spring.datasource.hikari.maximum-pool-size:10}") int poolSize) {
    if (poolSize < 10) {
      throw new IllegalStateException("Grading worker requires at least ten database connections");
    }
    return new WorkerConsumptionControl(jobs, leases, container(connection, process));
  }

  @Bean
  HealthIndicator gradingWorkerHealthIndicator(WorkerConsumptionControl control) {
    return () -> control.ready() ? Health.up().build() : Health.down().build();
  }
}
