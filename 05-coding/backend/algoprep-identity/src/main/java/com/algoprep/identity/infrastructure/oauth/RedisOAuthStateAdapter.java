package com.algoprep.identity.infrastructure.oauth;

import com.algoprep.identity.application.ports.out.OAuthStatePort;
import java.time.Duration;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/** {@code identity:oauthstate:<state>} for ten minutes; consuming deletes it (DD F1 BR-03). */
@Component
class RedisOAuthStateAdapter implements OAuthStatePort {
  private static final Duration TTL = Duration.ofMinutes(10);
  private static final String PREFIX = "identity:oauthstate:";

  private final ObjectProvider<StringRedisTemplate> redis;

  RedisOAuthStateAdapter(ObjectProvider<StringRedisTemplate> redis) {
    this.redis = redis;
  }

  @Override
  public void store(String state) {
    redis.getObject().opsForValue().set(PREFIX + state, "1", TTL);
  }

  @Override
  public boolean consume(String state) {
    return redis.getObject().opsForValue().getAndDelete(PREFIX + state) != null;
  }
}
