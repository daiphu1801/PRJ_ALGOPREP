package com.algoprep.identity.infrastructure.messaging;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Declares the exchange only. The broker admin of the Rabbit auto-configuration creates it on first
 * connection; queues belong to the consuming modules.
 */
@Configuration
class ClassEventExchangeConfig {
  static final String EXCHANGE = "algoprep.identity.events";

  @Bean
  TopicExchange identityEventsExchange() {
    return new TopicExchange(EXCHANGE, true, false);
  }
}
