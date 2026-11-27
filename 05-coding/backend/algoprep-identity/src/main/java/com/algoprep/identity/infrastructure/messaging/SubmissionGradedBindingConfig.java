package com.algoprep.identity.infrastructure.messaging;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * The queue belongs to this module; the exchange is declared with the arguments the judge relay
 * uses (DD api F4 section 25.1: topic, key {@code judge.submission.graded}), so whichever side
 * connects first creates it. The broker admin of the Rabbit auto-configuration declares all three.
 */
@Configuration
class SubmissionGradedBindingConfig {
  static final String QUEUE = "identity.submission-graded";
  static final String EXCHANGE = "algoprep.domain-events";
  static final String ROUTING_KEY = "judge.submission.graded";
  static final String ACCEPTED_QUEUE = "identity.submission-accepted";
  static final String ACCEPTED_ROUTING_KEY = "judge.submission.accepted";

  @Bean
  Queue submissionGradedQueue() {
    return new Queue(QUEUE, true);
  }

  @Bean
  Queue submissionAcceptedQueue() {
    return new Queue(ACCEPTED_QUEUE, true);
  }

  @Bean
  Binding submissionAcceptedBinding(
      Queue submissionAcceptedQueue, TopicExchange domainEventsExchange) {
    return BindingBuilder.bind(submissionAcceptedQueue)
        .to(domainEventsExchange)
        .with(ACCEPTED_ROUTING_KEY);
  }

  @Bean
  TopicExchange domainEventsExchange() {
    return new TopicExchange(EXCHANGE, true, false);
  }

  @Bean
  Binding submissionGradedBinding(Queue submissionGradedQueue, TopicExchange domainEventsExchange) {
    return BindingBuilder.bind(submissionGradedQueue).to(domainEventsExchange).with(ROUTING_KEY);
  }
}
