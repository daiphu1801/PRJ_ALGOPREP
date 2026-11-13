package com.algoprep.identity.infrastructure.ratelimit;

import com.algoprep.identity.application.ports.out.ExportRateLimitPort;
import java.time.Duration;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/** {@code SET NX EX} is one atomic step, so two concurrent exports cannot both win. */
@Component
class RedisExportRateLimitAdapter implements ExportRateLimitPort {
  private static final Duration WINDOW = Duration.ofMinutes(5);
  private final ObjectProvider<StringRedisTemplate> redis;

  RedisExportRateLimitAdapter(ObjectProvider<StringRedisTemplate> redis) {
    this.redis = redis;
  }

  @Override
  public boolean tryAcquire(UUID userId, String scope) {
    Boolean taken = redis.getObject().opsForValue().setIfAbsent(key(userId, scope), "1", WINDOW);
    return Boolean.TRUE.equals(taken);
  }

  @Override
  public void release(UUID userId, String scope) {
    redis.getObject().delete(key(userId, scope));
  }

  private static String key(UUID userId, String scope) {
    return "identity:export:" + userId + ":" + scope;
  }
}
