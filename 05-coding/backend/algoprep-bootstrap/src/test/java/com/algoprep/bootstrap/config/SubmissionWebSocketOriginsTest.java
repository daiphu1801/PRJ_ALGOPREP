package com.algoprep.bootstrap.config;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.StompWebSocketEndpointRegistration;

class SubmissionWebSocketOriginsTest {
  @Test
  void defaultKeepsSameOriginPolicy() {
    var registry = mock(StompEndpointRegistry.class);
    var endpoint = mock(StompWebSocketEndpointRegistration.class);
    when(registry.addEndpoint("/ws")).thenReturn(endpoint);
    var config =
        new SubmissionWebSocketConfig(
            mock(JwtDecoder.class), jwt -> null, mock(ThreadPoolTaskScheduler.class));
    config.registerStompEndpoints(registry);
    verify(endpoint, never()).setAllowedOrigins(any(String[].class));
  }

  @Test
  void explicitOriginsReachTheHandshakeRegistrationWithoutAWildcard() {
    var registry = mock(StompEndpointRegistry.class);
    var endpoint = mock(StompWebSocketEndpointRegistration.class);
    when(registry.addEndpoint("/ws")).thenReturn(endpoint);
    var config =
        new SubmissionWebSocketConfig(
            mock(JwtDecoder.class), jwt -> null, mock(ThreadPoolTaskScheduler.class));
    ReflectionTestUtils.setField(
        config, "allowedOrigins", " http://localhost:3000, ,http://127.0.0.1:3000 ");
    config.registerStompEndpoints(registry);
    verify(endpoint).setAllowedOrigins("http://localhost:3000", "http://127.0.0.1:3000");
  }
}
