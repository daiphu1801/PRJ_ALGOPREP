package com.algoprep.bootstrap.config;

import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.task.SimpleAsyncTaskSchedulerBuilder;
import org.springframework.boot.task.ThreadPoolTaskSchedulerBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.messaging.Message;
import org.springframework.messaging.converter.JacksonJsonMessageConverter;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration;
import org.springframework.web.socket.handler.WebSocketHandlerDecorator;
import org.springframework.web.socket.messaging.StompSubProtocolErrorHandler;

@Configuration
@EnableWebSocketMessageBroker
class SubmissionWebSocketConfig implements WebSocketMessageBrokerConfigurer {
  private final JwtDecoder decoder;
  private final Converter<Jwt, AbstractAuthenticationToken> converter;
  private final SubmissionSocketSessions sessions;

  @Value("${judge.websocket.allowed-origins:}")
  private String allowedOrigins = "";

  SubmissionWebSocketConfig(
      JwtDecoder decoder,
      Converter<Jwt, AbstractAuthenticationToken> converter,
      @Qualifier("submissionSocketExpiryScheduler") ThreadPoolTaskScheduler scheduler) {
    this.decoder = decoder;
    this.converter = converter;
    this.sessions = new SubmissionSocketSessions(scheduler);
  }

  @Bean
  static TaskScheduler taskScheduler(
      SimpleAsyncTaskSchedulerBuilder virtualBuilder,
      ThreadPoolTaskSchedulerBuilder platformBuilder,
      @Value("${spring.threads.virtual.enabled:false}") boolean virtualThreads) {
    // STOMP adds schedulers that make Boot's default back off; retain the application job
    // scheduler.
    return virtualThreads ? virtualBuilder.build() : platformBuilder.build();
  }

  @Bean
  static ThreadPoolTaskScheduler submissionSocketExpiryScheduler() {
    ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
    scheduler.setThreadNamePrefix("submission-socket-expiry-");
    scheduler.setPoolSize(1);
    scheduler.setRemoveOnCancelPolicy(true);
    return scheduler;
  }

  @Override
  public void registerStompEndpoints(StompEndpointRegistry registry) {
    registry.setErrorHandler(new GenericErrorHandler());
    var endpoint = registry.addEndpoint("/ws");
    String[] origins =
        Arrays.stream(allowedOrigins.split(","))
            .map(String::trim)
            .filter(origin -> !origin.isEmpty())
            .toArray(String[]::new);
    // Empty configuration retains Spring's same-origin policy; local dev uses an explicit list.
    if (origins.length > 0) {
      endpoint.setAllowedOrigins(origins);
    }
  }

  @Override
  public void configureMessageBroker(MessageBrokerRegistry registry) {
    registry.enableSimpleBroker("/queue", "/topic");
    registry.setUserDestinationPrefix("/user");
    registry.setPreservePublishOrder(true);
  }

  @Override
  public boolean configureMessageConverters(List<MessageConverter> converters) {
    converters.add(new JacksonJsonMessageConverter());
    return false;
  }

  @Override
  public void configureClientInboundChannel(ChannelRegistration registration) {
    registration.interceptors(new SubmissionStompInterceptor(decoder, converter, sessions));
  }

  @Override
  public void configureWebSocketTransport(WebSocketTransportRegistration registration) {
    registration.setMessageSizeLimit(8192).setSendBufferSizeLimit(65536).setSendTimeLimit(5000);
    registration.addDecoratorFactory(this::decorate);
  }

  private WebSocketHandler decorate(WebSocketHandler handler) {
    return new WebSocketHandlerDecorator(handler) {
      @Override
      public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        sessions.opened(session);
        super.afterConnectionEstablished(session);
      }

      @Override
      public void afterConnectionClosed(WebSocketSession session, CloseStatus status)
          throws Exception {
        sessions.closed(session.getId());
        super.afterConnectionClosed(session, status);
      }
    };
  }

  private static final class GenericErrorHandler extends StompSubProtocolErrorHandler {
    @Override
    public Message<byte[]> handleClientMessageProcessingError(
        Message<byte[]> client, Throwable ex) {
      StompHeaderAccessor error = StompHeaderAccessor.create(StompCommand.ERROR);
      error.setMessage("Request denied");
      return MessageBuilder.createMessage(new byte[0], error.getMessageHeaders());
    }
  }
}
