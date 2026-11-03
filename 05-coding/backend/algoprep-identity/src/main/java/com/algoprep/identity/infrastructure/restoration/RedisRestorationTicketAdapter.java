package com.algoprep.identity.infrastructure.restoration;

import com.algoprep.identity.application.ports.out.RestorationTicketPort;
import com.algoprep.identity.domain.model.RestorationGrant;
import java.time.DateTimeException;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/** Redis TTL bounds storage lifetime; the database timestamp also bounds redemption after locks. */
@Component
class RedisRestorationTicketAdapter implements RestorationTicketPort {
  private final ObjectProvider<StringRedisTemplate> redis;

  RedisRestorationTicketAdapter(ObjectProvider<StringRedisTemplate> redis) {
    this.redis = redis;
  }

  @Override
  public void save(String ticketHash, RestorationGrant grant) {
    String value = grant.userId() + "|" + grant.deactivatedAt() + "|" + grant.expiresAt();
    if (!Boolean.TRUE.equals(
        redis
            .getObject()
            .opsForValue()
            .setIfAbsent(key(ticketHash), value, Duration.ofSeconds(300)))) {
      throw new IllegalStateException("Restoration ticket could not be stored");
    }
  }

  @Override
  public Optional<RestorationGrant> find(String ticketHash) {
    String value = redis.getObject().opsForValue().get(key(ticketHash));
    if (value == null) {
      return Optional.empty();
    }
    String[] fields = value.split("\\|", -1);
    if (fields.length != 3) {
      return Optional.empty();
    }
    try {
      return Optional.of(
          new RestorationGrant(
              UUID.fromString(fields[0]), Instant.parse(fields[1]), Instant.parse(fields[2])));
    } catch (IllegalArgumentException | DateTimeException ex) {
      return Optional.empty();
    }
  }

  @Override
  public void delete(String ticketHash) {
    redis.getObject().delete(key(ticketHash));
  }

  private static String key(String hash) {
    return "identity:restore:" + hash;
  }
}
