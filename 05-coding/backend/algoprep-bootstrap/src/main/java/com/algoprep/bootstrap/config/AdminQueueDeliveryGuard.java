package com.algoprep.bootstrap.config;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.support.ChannelInterceptor;

/** Rechecks in-memory membership and JWT expiry even if physical socket close is delayed. */
final class AdminQueueDeliveryGuard implements ChannelInterceptor {
  private final AdminQueueSubscriptionView audience;

  AdminQueueDeliveryGuard(AdminQueueSubscriptionView audience) {
    this.audience = audience;
  }

  @Override
  public Message<?> preSend(Message<?> message, MessageChannel channel) {
    var headers = message.getHeaders();
    if (SimpMessageType.MESSAGE != SimpMessageHeaderAccessor.getMessageType(headers)
        || !AdminQueueSubscriptionView.DESTINATION.equals(
            SimpMessageHeaderAccessor.getDestination(headers))) {
      return message;
    }
    return audience.accepts(
            SimpMessageHeaderAccessor.getSessionId(headers),
            SimpMessageHeaderAccessor.getSubscriptionId(headers))
        ? message
        : null;
  }
}
