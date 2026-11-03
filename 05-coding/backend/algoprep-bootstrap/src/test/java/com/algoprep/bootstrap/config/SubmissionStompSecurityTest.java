package com.algoprep.bootstrap.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.core.convert.converter.Converter;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

class SubmissionStompSecurityTest {
  private final JwtDecoder decoder = mock(JwtDecoder.class);

  @SuppressWarnings("unchecked")
  private final Converter<Jwt, AbstractAuthenticationToken> converter = mock(Converter.class);

  private final SubmissionSocketSessions sessions = mock(SubmissionSocketSessions.class);
  private final SubmissionStompInterceptor interceptor =
      new SubmissionStompInterceptor(decoder, converter, sessions);
  private final Jwt jwt =
      Jwt.withTokenValue("signed")
          .header("alg", "RS256")
          .subject(UUID.randomUUID().toString())
          .expiresAt(Instant.now().plusSeconds(60))
          .build();

  @Test
  void connectsByDecodedSubjectAndRemovesBearerHeader() {
    var headers = connect("Bearer signed");
    var auth = authentication("ROLE_STUDENT");
    when(decoder.decode("signed")).thenReturn(jwt);
    when(converter.convert(jwt)).thenReturn(auth);
    interceptor.preSend(message(headers), mock(MessageChannel.class));
    assertThat(headers.getUser()).isSameAs(auth);
    assertThat(headers.getUser().getName()).isEqualTo(jwt.getSubject());
    assertThat(headers.getNativeHeader("Authorization")).isNull();
  }

  @Test
  void rejectsDuplicateAuthorizationHeaderAndUnsupportedVersion() {
    var duplicate = connect("Bearer signed");
    duplicate.addNativeHeader("Authorization", "Bearer another");
    denied(duplicate);
    var legacy = connect("Bearer signed");
    legacy.setAcceptVersion("1.1");
    denied(legacy);
  }

  @Test
  void rejectsNullAuthenticationAndMissingExpiryWithoutLeakingCause() {
    when(decoder.decode("signed")).thenReturn(jwt);
    denied(connect("Bearer signed"));
    when(decoder.decode("signed"))
        .thenReturn(
            Jwt.withTokenValue("signed")
                .header("alg", "RS256")
                .subject(jwt.getSubject())
                .claim("extra", "value")
                .build());
    denied(connect("Bearer signed"));
    when(decoder.decode("signed")).thenThrow(new IllegalArgumentException("SECRET TOKEN"));
    denied(connect("Bearer signed"));
  }

  @Test
  void adminSubscriptionRechecksLiveAuthoritiesAndSchedulesExpiry() {
    var headers = subscription("/topic/admin/queue", authentication("ROLE_ADMIN"));
    when(converter.convert(jwt))
        .thenReturn(authentication("ROLE_ADMIN", "JUDGE_QUEUE_MONITOR:READ"));
    interceptor.preSend(message(headers), mock(MessageChannel.class));
    verify(sessions).expireAdmin("session", jwt.getExpiresAt());
  }

  @ParameterizedTest
  @ValueSource(strings = {"STUDENT", "INSTRUCTOR"})
  void queueReadCellWithoutAdminCategoryIsInsufficient(String category) {
    var auth = authentication("ROLE_" + category, "JUDGE_QUEUE_MONITOR:READ");
    when(converter.convert(jwt)).thenReturn(auth);
    denied(subscription("/topic/admin/queue", auth));
  }

  @Test
  void subscribesOnlyWithAuthenticatedPrincipalAndAllowsCleanupFrames() {
    denied(subscription("/user/queue/submissions", null));
    var headers = StompHeaderAccessor.create(StompCommand.DISCONNECT);
    assertThat(interceptor.preSend(message(headers), mock(MessageChannel.class))).isNotNull();
    headers = StompHeaderAccessor.create(StompCommand.UNSUBSCRIBE);
    headers.setUser(authentication("ROLE_STUDENT"));
    assertThat(interceptor.preSend(message(headers), mock(MessageChannel.class))).isNotNull();
  }

  private void denied(StompHeaderAccessor headers) {
    assertThatThrownBy(() -> interceptor.preSend(message(headers), mock(MessageChannel.class)))
        .isInstanceOf(MessageDeliveryException.class)
        .hasMessage("Request denied")
        .hasNoCause();
  }

  private JwtAuthenticationToken authentication(String... authorities) {
    return new JwtAuthenticationToken(
        jwt,
        java.util.Arrays.stream(authorities).map(SimpleGrantedAuthority::new).toList(),
        jwt.getSubject());
  }

  private static StompHeaderAccessor connect(String bearer) {
    var headers = StompHeaderAccessor.create(StompCommand.CONNECT);
    headers.setAcceptVersion("1.2");
    headers.setNativeHeader("Authorization", bearer);
    return headers;
  }

  private static StompHeaderAccessor subscription(String destination, JwtAuthenticationToken auth) {
    var headers = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
    headers.setDestination(destination);
    headers.setSessionId("session");
    headers.setUser(auth);
    return headers;
  }

  private static Message<byte[]> message(StompHeaderAccessor headers) {
    headers.setLeaveMutable(true);
    return MessageBuilder.createMessage(new byte[0], headers.getMessageHeaders());
  }
}
