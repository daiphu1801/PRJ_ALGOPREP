package com.algoprep.identity.infrastructure.messaging;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Queue and binding for {@code ProblemAssignedToClass} (DD api F2 section 53, key {@code
 * problem.assignment.assigned}). The exchange bean is the one declared by {@link
 * SubmissionGradedBindingConfig}. Bean names carry a "problemAssigned" prefix so they cannot clash
 * with the beans of another module.
 */
@Configuration("identityProblemAssignedBindingConfig")
class ProblemAssignedBindingConfig {
  static final String QUEUE = "identity.problem-assigned";
  static final String ROUTING_KEY = "problem.assignment.assigned";

  @Bean
  Queue identityProblemAssignedQueue() {
    return new Queue(QUEUE, true);
  }

  @Bean
  Binding identityProblemAssignedBinding(
      Queue identityProblemAssignedQueue, TopicExchange domainEventsExchange) {
    return BindingBuilder.bind(identityProblemAssignedQueue)
        .to(domainEventsExchange)
        .with(ROUTING_KEY);
  }
}
