package com.algoprep.bootstrap.judge;

import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.simp.broker.SimpleBrokerMessageHandler;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ExecutorChannelInterceptor;
import org.springframework.messaging.support.ExecutorSubscribableChannel;
import org.springframework.messaging.support.MessageHeaderAccessor;

@TestConfiguration
class QueueStreamBrokerBarrier {
  @Bean
  SmartInitializingSingleton queueStreamBarrier(
      @Qualifier("clientInboundChannel") ExecutorSubscribableChannel inbound) {
    return () ->
        inbound.addInterceptor(
            new ExecutorChannelInterceptor() {
              @Override
              public void afterMessageHandled(
                  Message<?> message,
                  MessageChannel channel,
                  MessageHandler handler,
                  Exception error) {
                var headers = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
                if (handler instanceof SimpleBrokerMessageHandler
                    && error == null
                    && headers != null) {
                  if (headers.getCommand() == StompCommand.SUBSCRIBE) {
                    QueueStreamWireClient.handled(headers.getSubscriptionId());
                  } else if (headers.getCommand() == StompCommand.UNSUBSCRIBE) {
                    QueueStreamWireClient.handled("unsubscribe:" + headers.getSubscriptionId());
                  }
                }
              }
            });
  }
}
