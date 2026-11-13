package com.algoprep.bootstrap.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.user.SimpSession;
import org.springframework.messaging.simp.user.SimpSubscription;
import org.springframework.messaging.simp.user.SimpSubscriptionMatcher;
import org.springframework.messaging.simp.user.SimpUser;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

class AdminQueueDeliveryGuardTest {
  @SuppressWarnings("unchecked")
  private final ObjectProvider<SimpUserRegistry> provider = mock(ObjectProvider.class);

  private final SimpUserRegistry registry = mock(SimpUserRegistry.class);
  private final AdminQueueSubscriptionView view = new AdminQueueSubscriptionView(provider);
  private final AdminQueueDeliveryGuard guard = new AdminQueueDeliveryGuard(view);

  private SimpSubscription subscription(String destination, Instant expiry) {
    var sub = mock(SimpSubscription.class);
    var session = mock(SimpSession.class);
    var user = mock(SimpUser.class);
    var builder = Jwt.withTokenValue("test-token").header("alg", "RS256").subject("operator");
    if (expiry != null) {
      builder.expiresAt(expiry);
    }
    when(user.getPrincipal())
        .thenReturn(
            new JwtAuthenticationToken(
                builder.build(), List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
    when(session.getUser()).thenReturn(user);
    when(session.getId()).thenReturn("session");
    when(sub.getSession()).thenReturn(session);
    when(sub.getId()).thenReturn("subscription");
    when(sub.getDestination()).thenReturn(destination);
    return sub;
  }

  private void subscriptions(Set<SimpSubscription> subs) {
    when(provider.getIfAvailable()).thenReturn(registry);
    doAnswer(
            inv -> {
              SimpSubscriptionMatcher matcher = inv.getArgument(0);
              return subs.stream().filter(matcher::match).collect(Collectors.toSet());
            })
        .when(registry)
        .findSubscriptions(any());
  }

  private Message<?> frame(String destination, String session, String subscription) {
    var headers = SimpMessageHeaderAccessor.create(SimpMessageType.MESSAGE);
    headers.setDestination(destination);
    headers.setSessionId(session);
    headers.setSubscriptionId(subscription);
    return MessageBuilder.createMessage(new byte[0], headers.getMessageHeaders());
  }

  @Test
  void missingRegistryAndNoAcceptedSubscribersStayIdle() {
    assertThat(view.hasSubscribers()).isFalse();
    assertThat(
            guard.preSend(
                frame(AdminQueueSubscriptionView.DESTINATION, "session", "subscription"), null))
        .isNull();
    subscriptions(Set.of(subscription("/user/queue/submissions", Instant.now().plusSeconds(60))));
    assertThat(view.hasSubscribers()).isFalse();
  }

  @Test
  void expiredOrMissingExpiryNeverCountsOrReceivesEvenBeforeSocketCleanup() {
    for (Instant expiry : new Instant[] {Instant.now().minusSeconds(1), null}) {
      subscriptions(Set.of(subscription(AdminQueueSubscriptionView.DESTINATION, expiry)));
      assertThat(view.hasSubscribers()).isFalse();
      assertThat(
              guard.preSend(
                  frame(AdminQueueSubscriptionView.DESTINATION, "session", "subscription"), null))
          .isNull();
    }
  }

  @Test
  void onlyAnExactLiveSubscriptionReceivesTheBatch() {
    subscriptions(
        Set.of(
            subscription(AdminQueueSubscriptionView.DESTINATION, Instant.now().plusSeconds(60))));
    assertThat(view.hasSubscribers()).isTrue();
    var valid = frame(AdminQueueSubscriptionView.DESTINATION, "session", "subscription");
    assertThat(guard.preSend(valid, null)).isSameAs(valid);
    for (var invalid :
        List.of(
            frame(AdminQueueSubscriptionView.DESTINATION, "other", "subscription"),
            frame(AdminQueueSubscriptionView.DESTINATION, "session", "other"),
            frame(AdminQueueSubscriptionView.DESTINATION, null, "subscription"),
            frame(AdminQueueSubscriptionView.DESTINATION, "session", null))) {
      assertThat(guard.preSend(invalid, null)).isNull();
    }
    subscriptions(Set.of());
    assertThat(guard.preSend(valid, null)).isNull();
  }

  @Test
  void studentMessagesAndControlFramesAreUnchanged() {
    var student = frame("/queue/submissions", "session", "subscription");
    assertThat(guard.preSend(student, null)).isSameAs(student);
    var headers = SimpMessageHeaderAccessor.create(SimpMessageType.CONNECT_ACK);
    var connected = MessageBuilder.createMessage(new byte[0], headers.getMessageHeaders());
    assertThat(guard.preSend(connected, null)).isSameAs(connected);
  }
}
