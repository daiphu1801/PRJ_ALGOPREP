package com.algoprep.bootstrap.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
class AdminQueueStreamConfig implements WebSocketMessageBrokerConfigurer {
  private final AdminQueueSubscriptionView audience;

  AdminQueueStreamConfig(AdminQueueSubscriptionView audience) {
    this.audience = audience;
  }

  @Override
  public void configureClientOutboundChannel(ChannelRegistration registration) {
    registration.interceptors(new AdminQueueDeliveryGuard(audience));
  }
}
