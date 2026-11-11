package com.algoprep.identity.infrastructure.messaging;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Queue and binding for {@code SolutionReviewCompleted} (DD api F5 section 33, key {@code
 * ai.solution-review.completed}). The exchange bean is the one declared by {@link
 * SubmissionGradedBindingConfig}. Bean names carry a "solutionReviewCompleted" prefix so they
 * cannot clash with the beans of another module.
 */
@Configuration("identitySolutionReviewCompletedBindingConfig")
class SolutionReviewCompletedBindingConfig {
  static final String QUEUE = "identity.solution-review-completed";
  static final String ROUTING_KEY = "ai.solution-review.completed";

  @Bean
  Queue identitySolutionReviewCompletedQueue() {
    return new Queue(QUEUE, true);
  }

  @Bean
  Binding identitySolutionReviewCompletedBinding(
      Queue identitySolutionReviewCompletedQueue, TopicExchange domainEventsExchange) {
    return BindingBuilder.bind(identitySolutionReviewCompletedQueue)
        .to(domainEventsExchange)
        .with(ROUTING_KEY);
  }
}
