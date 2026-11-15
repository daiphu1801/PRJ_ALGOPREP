package com.algoprep.bootstrap.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Arrays;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.core.convert.converter.Converter;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

class SubmissionStompOutboundInterceptorTest {
  @SuppressWarnings("unchecked")
  private final Converter<Jwt, AbstractAuthenticationToken> converter = mock(Converter.class);

  private final SubmissionSocketSessions sessions = mock(SubmissionSocketSessions.class);
  private final SubmissionStompOutboundInterceptor interceptor =
      new SubmissionStompOutboundInterceptor(converter, sessions);
  private final Jwt jwt = jwt(Instant.now().plusSeconds(120));

  @Test
  void protocolFramesPassWithoutAuthorizationReads() {
    var frame = frame(SimpMessageType.CONNECT_ACK, "session", null);
    assertThat(interceptor.preSend(frame, null)).isSameAs(frame);
    verifyNoInteractions(converter, sessions);
  }

  @Test
  void recipientSessionIsRequired() {
    assertThat(
            interceptor.preSend(frame(SimpMessageType.MESSAGE, null, "/topic/admin/queue"), null))
        .isNull();
    verifyNoInteractions(converter, sessions);
  }

  @Test
  void adminBatchUsesCurrentAuthoritiesRatherThanConnectTimePermissions() {
    var original = auth(jwt, "ROLE_ADMIN", "JUDGE_QUEUE_MONITOR:READ");
    when(sessions.authentication("session")).thenReturn(original);
    when(converter.convert(jwt)).thenReturn(auth(jwt, "ROLE_ADMIN", "JUDGE_QUEUE_MONITOR:READ"));
    var frame = frame(SimpMessageType.MESSAGE, "session", "/topic/admin/queue");
    assertThat(interceptor.preSend(frame, null)).isSameAs(frame);
    when(converter.convert(jwt)).thenReturn(auth(jwt, "ROLE_ADMIN"));
    assertThat(interceptor.preSend(frame, null)).isNull();
    verify(sessions).reject("session");
  }

  @ParameterizedTest
  @ValueSource(strings = {"ROLE_STUDENT", "ROLE_INSTRUCTOR"})
  void queueReadCellAloneCannotKeepAnAdminSubscription(String category) {
    when(sessions.authentication("session")).thenReturn(auth(jwt, "ROLE_ADMIN"));
    when(converter.convert(jwt)).thenReturn(auth(jwt, category, "JUDGE_QUEUE_MONITOR:READ"));
    assertThat(
            interceptor.preSend(
                frame(SimpMessageType.MESSAGE, "session", "/topic/admin/queue"), null))
        .isNull();
    verify(sessions).reject("session");
  }

  @Test
  void studentReceivesOnlyTheirResolvedSubmissionDestination() {
    when(sessions.authentication("session")).thenReturn(auth(jwt, "ROLE_STUDENT"));
    when(converter.convert(jwt)).thenReturn(auth(jwt, "ROLE_STUDENT"));
    var own = frame(SimpMessageType.MESSAGE, "session", "/queue/submissions-usersession");
    assertThat(interceptor.preSend(own, null)).isSameAs(own);
    assertThat(
            interceptor.preSend(
                frame(SimpMessageType.MESSAGE, "session", "/queue/submissions-userother"), null))
        .isNull();
    verify(sessions).reject("session");
  }

  @Test
  void revokedStudentRoleCannotReceiveSubmissionData() {
    when(sessions.authentication("session")).thenReturn(auth(jwt, "ROLE_STUDENT"));
    when(converter.convert(jwt)).thenReturn(auth(jwt, "ROLE_INSTRUCTOR"));
    assertThat(
            interceptor.preSend(
                frame(SimpMessageType.MESSAGE, "session", "/queue/submissions-usersession"), null))
        .isNull();
    verify(sessions).reject("session");
  }

  @Test
  void accountOrAuthorizationStoreFailureDropsPayloadAndClosesSocket() {
    when(sessions.authentication("session")).thenReturn(auth(jwt, "ROLE_ADMIN"));
    when(converter.convert(jwt)).thenThrow(new IllegalStateException("private SQL and token"));
    assertThat(
            interceptor.preSend(
                frame(SimpMessageType.MESSAGE, "session", "/topic/admin/queue"), null))
        .isNull();
    verify(sessions).reject("session");
  }

  @Test
  void missingOrUnauthenticatedCurrentIdentityIsRejected() {
    when(sessions.authentication("session")).thenReturn(auth(jwt, "ROLE_ADMIN"));
    var frame = frame(SimpMessageType.MESSAGE, "session", "/topic/admin/queue");
    assertThat(interceptor.preSend(frame, null)).isNull();
    AbstractAuthenticationToken unauthenticated = mock(AbstractAuthenticationToken.class);
    when(converter.convert(jwt)).thenReturn(unauthenticated);
    assertThat(interceptor.preSend(frame, null)).isNull();
  }

  @Test
  void missingSessionAuthenticationIsRejectedWithoutConverterRead() {
    assertThat(
            interceptor.preSend(
                frame(SimpMessageType.MESSAGE, "session", "/topic/admin/queue"), null))
        .isNull();
    verify(sessions).reject("session");
    verifyNoInteractions(converter);
  }

  @Test
  void missingOrExpiredJwtExpiryIsRejectedWithoutConverterRead() {
    var frame = frame(SimpMessageType.MESSAGE, "session", "/topic/admin/queue");
    when(sessions.authentication("session")).thenReturn(auth(jwt(null), "ROLE_ADMIN"));
    assertThat(interceptor.preSend(frame, null)).isNull();
    when(sessions.authentication("session"))
        .thenReturn(auth(jwt(Instant.now().minusSeconds(1)), "ROLE_ADMIN"));
    assertThat(interceptor.preSend(frame, null)).isNull();
    verifyNoInteractions(converter);
  }

  private static Jwt jwt(Instant expiry) {
    var builder =
        Jwt.withTokenValue("signed").header("alg", "RS256").subject(UUID.randomUUID().toString());
    if (expiry != null) {
      builder.expiresAt(expiry);
    }
    return builder.build();
  }

  private static JwtAuthenticationToken auth(Jwt jwt, String... authorities) {
    return new JwtAuthenticationToken(
        jwt, Arrays.stream(authorities).map(SimpleGrantedAuthority::new).toList());
  }

  private static Message<byte[]> frame(SimpMessageType type, String session, String destination) {
    var headers = SimpMessageHeaderAccessor.create(type);
    headers.setSessionId(session);
    headers.setDestination(destination);
    return MessageBuilder.createMessage(new byte[] {1}, headers.getMessageHeaders());
  }
}
