package com.algoprep.harness.infrastructure.ratelimit;

import com.algoprep.harness.application.ports.out.PreviewRateLimitPort;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

/**
 * Sliding window of 10 calls per second per user, one atomic script. The key is a throttle counter,
 * never a cache or a source of truth. Redis down lets the call through: P6 is pure CPU and its body
 * is capped (DD F3 3.7).
 */
@Component
class RedisPreviewRateLimitAdapter implements PreviewRateLimitPort {
  private static final Logger LOG = Logger.getLogger(RedisPreviewRateLimitAdapter.class.getName());
  private static final DefaultRedisScript<Long> ACQUIRE =
      new DefaultRedisScript<>(
          """
      local t=redis.call('TIME')
      local now=tonumber(t[1])*1000+math.floor(tonumber(t[2])/1000)
      redis.call('ZREMRANGEBYSCORE',KEYS[1],'-inf',now-1000)
      if redis.call('ZCARD',KEYS[1])>=10 then
        return 1
      end
      redis.call('ZADD',KEYS[1],now,ARGV[1])
      redis.call('PEXPIRE',KEYS[1],2000)
      return 0
      """,
          Long.class);
  private final ObjectProvider<StringRedisTemplate> redis;

  RedisPreviewRateLimitAdapter(ObjectProvider<StringRedisTemplate> redis) {
    this.redis = redis;
  }

  @Override
  public long acquire(UUID userId) {
    try {
      Long wait =
          redis
              .getObject()
              .execute(
                  ACQUIRE, List.of("harness:preview:rate:" + userId), UUID.randomUUID().toString());
      return wait == null ? 0 : wait;
    } catch (RuntimeException unavailable) {
      LOG.log(Level.WARNING, "Signature preview throttle unavailable; call allowed");
      return 0;
    }
  }
}
