package com.algoprep.bootstrap.config;

import java.time.Instant;
import org.springframework.core.convert.converter.Converter;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;

/** Revalidates the recipient before a queued application message reaches the transport. */
final class SubmissionStompOutboundInterceptor implements ChannelInterceptor {
  private final Converter<Jwt, AbstractAuthenticationToken> converter;
  private final SubmissionSocketSessions sessions;

  SubmissionStompOutboundInterceptor(
      Converter<Jwt, AbstractAuthenticationToken> converter, SubmissionSocketSessions sessions) {
    this.converter = converter;
    this.sessions = sessions;
  }

  @Override
  public Message<?> preSend(Message<?> message, MessageChannel channel) {
    if (SimpMessageHeaderAccessor.getMessageType(message.getHeaders()) != SimpMessageType.MESSAGE) {
      return message;
    }
    String sessionId = SimpMessageHeaderAccessor.getSessionId(message.getHeaders());
    if (sessionId == null) {
      return null;
    }
    try {
      var previous = sessions.authentication(sessionId);
      if (previous == null) {
        sessions.reject(sessionId);
        return null;
      }
      Jwt jwt = previous.getToken();
      Instant expiry = jwt.getExpiresAt();
      if (expiry == null || !expiry.isAfter(Instant.now())) {
        sessions.reject(sessionId);
        return null;
      }
      var current = converter.convert(jwt);
      String destination = SimpMessageHeaderAccessor.getDestination(message.getHeaders());
      boolean allowed =
          current != null
              && current.isAuthenticated()
              && ("/topic/admin/queue".equals(destination)
                  ? has(current, "ROLE_ADMIN") && has(current, "JUDGE_QUEUE_MONITOR:READ")
                  : ("/queue/submissions-user" + sessionId).equals(destination)
                      && has(current, "ROLE_STUDENT"));
      if (allowed) {
        return message;
      }
    } catch (RuntimeException error) {
      // The authorization source may be unavailable. Neither stale permissions nor the
      // original message/cause may escape through a framework exception or log.
    }
    sessions.reject(sessionId);
    return null;
  }

  private static boolean has(AbstractAuthenticationToken auth, String authority) {
    return auth.getAuthorities().stream().anyMatch(a -> authority.equals(a.getAuthority()));
  }
}
