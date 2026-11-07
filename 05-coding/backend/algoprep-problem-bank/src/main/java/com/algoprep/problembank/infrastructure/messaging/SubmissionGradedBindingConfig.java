package com.algoprep.problembank.infrastructure.messaging;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Own queue for {@code SubmissionGraded} (DD F2 section 59.2). The exchange is declared with the
 * arguments the judge relay uses (topic, durable), so whichever side connects first creates it; the
 * bean names differ from the identity module's so both configurations can coexist.
 */
@Configuration("problemSubmissionGradedBindingConfig")
class SubmissionGradedBindingConfig {
  static final String QUEUE = "problem-bank.submission-graded";
  static final String EXCHANGE = "algoprep.domain-events";
  static final String ROUTING_KEY = "judge.submission.graded";

  @Bean
  Queue problemSubmissionGradedQueue() {
    return new Queue(QUEUE, true);
  }

  @Bean
  TopicExchange problemDomainEventsExchange() {
    return new TopicExchange(EXCHANGE, true, false);
  }

  @Bean
  Binding problemSubmissionGradedBinding(
      Queue problemSubmissionGradedQueue, TopicExchange problemDomainEventsExchange) {
    return BindingBuilder.bind(problemSubmissionGradedQueue)
        .to(problemDomainEventsExchange)
        .with(ROUTING_KEY);
  }
}
