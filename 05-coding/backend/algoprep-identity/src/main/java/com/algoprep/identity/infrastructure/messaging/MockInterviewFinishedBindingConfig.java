package com.algoprep.identity.infrastructure.messaging;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Queue and binding for {@code MockInterviewFinished} (DD api F5 section 33, key {@code
 * ai.interview-session.finished}). The exchange bean is the one declared by {@link
 * SubmissionGradedBindingConfig}. Bean names carry a "mockInterviewFinished" prefix so they cannot
 * clash with the beans of another module.
 */
@Configuration("identityMockInterviewFinishedBindingConfig")
class MockInterviewFinishedBindingConfig {
  static final String QUEUE = "identity.mock-interview-finished";
  static final String ROUTING_KEY = "ai.interview-session.finished";

  @Bean
  Queue identityMockInterviewFinishedQueue() {
    return new Queue(QUEUE, true);
  }

  @Bean
  Binding identityMockInterviewFinishedBinding(
      Queue identityMockInterviewFinishedQueue, TopicExchange domainEventsExchange) {
    return BindingBuilder.bind(identityMockInterviewFinishedQueue)
        .to(domainEventsExchange)
        .with(ROUTING_KEY);
  }
}
