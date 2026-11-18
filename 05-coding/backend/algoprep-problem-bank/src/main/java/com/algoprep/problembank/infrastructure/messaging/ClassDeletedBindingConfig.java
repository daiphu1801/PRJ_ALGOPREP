package com.algoprep.problembank.infrastructure.messaging;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration("problemClassDeletedBindingConfig")
class ClassDeletedBindingConfig {
  static final String QUEUE = "problem-bank.class-deleted";

  @Bean
  Queue problemClassDeletedQueue() {
    return new Queue(QUEUE, true);
  }

  @Bean
  TopicExchange problemIdentityEventsExchange() {
    return new TopicExchange("algoprep.identity.events", true, false);
  }

  @Bean
  Binding problemClassDeletedBinding(
      Queue problemClassDeletedQueue, TopicExchange problemIdentityEventsExchange) {
    return BindingBuilder.bind(problemClassDeletedQueue)
        .to(problemIdentityEventsExchange)
        .with("class.deleted");
  }
}
