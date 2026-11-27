package com.algoprep.bootstrap.config;

import com.algoprep.judge.application.ports.out.QueueMonitorAudiencePort;
import java.time.Instant;
import java.util.Set;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.messaging.simp.user.SimpSubscription;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

/** Reads accepted subscriptions in memory; existing SUBSCRIBE security remains authoritative. */
@Component
class AdminQueueSubscriptionView implements QueueMonitorAudiencePort {
  static final String DESTINATION = "/topic/admin/queue";
  private final ObjectProvider<SimpUserRegistry> registry;

  AdminQueueSubscriptionView(ObjectProvider<SimpUserRegistry> registry) {
    this.registry = registry;
  }

  @Override
  public boolean hasSubscribers() {
    return subscriptions().stream().anyMatch(AdminQueueSubscriptionView::live);
  }

  boolean accepts(String sessionId, String subscriptionId) {
    if (sessionId == null || subscriptionId == null) {
      return false;
    }
    return subscriptions().stream()
        .anyMatch(
            s ->
                sessionId.equals(s.getSession().getId())
                    && subscriptionId.equals(s.getId())
                    && live(s));
  }

  private Set<SimpSubscription> subscriptions() {
    var users = registry.getIfAvailable();
    return users == null
        ? Set.of()
        : users.findSubscriptions(s -> DESTINATION.equals(s.getDestination()));
  }

  private static boolean live(SimpSubscription subscription) {
    var principal = subscription.getSession().getUser().getPrincipal();
    if (!(principal instanceof JwtAuthenticationToken token) || !token.isAuthenticated()) {
      return false;
    }
    var expiry = token.getToken().getExpiresAt();
    return expiry != null && expiry.isAfter(Instant.now());
  }
}
