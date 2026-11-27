package com.algoprep.problembank.infrastructure.messaging;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Durable integration backlog until a business consumer for problem lifecycle events is defined.
 */
@Configuration("problemLifecycleBindingConfig")
class ProblemLifecycleBindingConfig {
  @Bean
  Queue problemLifecyclePendingQueue() {
    // No expiry, TTL, overflow drop or automatic consumer: pending integration events are retained.
    return new Queue("problem-bank.lifecycle-pending", true, false, false);
  }

  @Bean
  TopicExchange problemLifecycleDomainEventsExchange() {
    return new TopicExchange("algoprep.domain-events", true, false);
  }

  @Bean
  Binding problemPublishedPendingBinding(
      @Qualifier("problemLifecyclePendingQueue") Queue queue,
      @Qualifier("problemLifecycleDomainEventsExchange") TopicExchange exchange) {
    return BindingBuilder.bind(queue).to(exchange).with("problem.problem.published");
  }

  @Bean
  Binding problemUnpublishedPendingBinding(
      @Qualifier("problemLifecyclePendingQueue") Queue queue,
      @Qualifier("problemLifecycleDomainEventsExchange") TopicExchange exchange) {
    return BindingBuilder.bind(queue).to(exchange).with("problem.problem.unpublished");
  }
}
