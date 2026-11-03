package com.algoprep.bootstrap.config;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.core.convert.converter.Converter;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/** Exact destination allow-list; HTTP handshake authentication grants no STOMP permission. */
final class SubmissionStompInterceptor implements ChannelInterceptor {
  private final JwtDecoder decoder;
  private final Converter<Jwt, AbstractAuthenticationToken> converter;
  private final SubmissionSocketSessions sessions;

  SubmissionStompInterceptor(
      JwtDecoder decoder,
      Converter<Jwt, AbstractAuthenticationToken> converter,
      SubmissionSocketSessions sessions) {
    this.decoder = decoder;
    this.converter = converter;
    this.sessions = sessions;
  }

  @Override
  public Message<?> preSend(Message<?> message, MessageChannel channel) {
    try {
      StompHeaderAccessor headers =
          MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
      if (headers == null) {
        throw new IllegalArgumentException();
      }
      StompCommand command = headers.getCommand();
      if (command == StompCommand.CONNECT || command == StompCommand.STOMP) {
        connect(headers);
      } else if (command == StompCommand.DISCONNECT) {
        return message;
      } else {
        if (!(headers.getUser() instanceof JwtAuthenticationToken auth)
            || !auth.isAuthenticated()) {
          throw new IllegalArgumentException();
        }
        if (command == StompCommand.SUBSCRIBE) {
          subscribe(headers, auth);
        } else if (command != StompCommand.UNSUBSCRIBE
            && headers.getMessageType() != SimpMessageType.HEARTBEAT) {
          throw new IllegalArgumentException();
        }
      }
      return message;
    } catch (RuntimeException error) {
      // Never attach the original frame or cause: both may contain the bearer token.
      throw new MessageDeliveryException("Request denied");
    }
  }

  private void connect(StompHeaderAccessor headers) {
    List<String> values = headers.getNativeHeader("Authorization");
    // Spring may render a rejected frame in its own error log, so redact before validation.
    headers.toNativeHeaderMap().keySet().stream()
        .filter(key -> "Authorization".equalsIgnoreCase(key))
        .toList()
        .forEach(headers::removeNativeHeader);
    if (values == null
        || values.size() != 1
        || !values.getFirst().startsWith("Bearer ")
        || !headers.getAcceptVersion().contains("1.2")) {
      throw new IllegalArgumentException();
    }
    Jwt jwt = decoder.decode(values.getFirst().substring(7));
    Instant expiresAt = Objects.requireNonNull(jwt.getExpiresAt());
    if (!expiresAt.isAfter(Instant.now())) {
      throw new IllegalArgumentException();
    }
    UUID.fromString(jwt.getSubject());
    AbstractAuthenticationToken auth = converter.convert(jwt);
    if (!(auth instanceof JwtAuthenticationToken) || !auth.isAuthenticated()) {
      throw new IllegalArgumentException();
    }
    headers.setUser(auth);
  }

  private void subscribe(StompHeaderAccessor headers, JwtAuthenticationToken auth) {
    Jwt authenticated = Objects.requireNonNull(auth.getToken());
    String destination = headers.getDestination();
    if ("/user/queue/submissions".equals(destination) && has(auth, "ROLE_STUDENT")) {
      return;
    }
    if ("/topic/admin/queue".equals(destination)) {
      AbstractAuthenticationToken current = converter.convert(authenticated);
      if (current != null
          && has(current, "ROLE_ADMIN")
          && has(current, "JUDGE_QUEUE_MONITOR:READ")) {
        String sessionId = Objects.requireNonNull(headers.getSessionId());
        Instant expiry = Objects.requireNonNull(authenticated.getExpiresAt());
        sessions.expireAdmin(sessionId, expiry);
        return;
      }
    }
    throw new IllegalArgumentException();
  }

  private static boolean has(AbstractAuthenticationToken auth, String authority) {
    return auth.getAuthorities().stream().anyMatch(a -> authority.equals(a.getAuthority()));
  }
}
