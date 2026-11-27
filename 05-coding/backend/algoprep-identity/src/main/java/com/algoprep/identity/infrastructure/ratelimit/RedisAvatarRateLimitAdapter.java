package com.algoprep.identity.infrastructure.ratelimit;

import com.algoprep.identity.application.ports.out.AvatarRateLimitPort;
import java.time.Duration;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Fixed one-hour window of 10 changes per user (BR-25). Fails open: any Redis problem is logged and
 * the change is allowed, because this limit protects storage churn, not a secret (BR-25 step 1,
 * {@code [SoT: Suy luận]}).
 *
 * <p>ponytail: INCR then EXPIRE are two commands, so a crash between them leaves a counter with no
 * TTL and that user is limited until the key is removed. Upgrade to a Lua script or SET NX EX plus
 * INCR if it ever matters; for a demo the window is rare and self-healing by manual DEL.
 */
@Component
class RedisAvatarRateLimitAdapter implements AvatarRateLimitPort {
  private static final Logger LOG = LoggerFactory.getLogger(RedisAvatarRateLimitAdapter.class);
  private static final Duration WINDOW = Duration.ofHours(1);
  private static final long LIMIT = 10;

  private final ObjectProvider<StringRedisTemplate> redis;

  RedisAvatarRateLimitAdapter(ObjectProvider<StringRedisTemplate> redis) {
    this.redis = redis;
  }

  @Override
  public boolean tryAcquire(UUID userId) {
    try {
      StringRedisTemplate template = redis.getObject();
      String key = "identity:avatar:rate:" + userId;
      Long count = template.opsForValue().increment(key);
      if (count == null) {
        return true;
      }
      if (count == 1) {
        template.expire(key, WINDOW);
      }
      return count <= LIMIT;
    } catch (RuntimeException failure) {
      LOG.warn("Avatar rate limit skipped because Redis failed", failure);
      return true;
    }
  }
}
