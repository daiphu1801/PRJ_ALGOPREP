package com.algoprep.identity.infrastructure.ratelimit;

import com.algoprep.identity.application.ports.out.LoginAttemptPort;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

/** BR-01: counter expires 15 minutes after the first failure; Redis errors fail open. */
@Component
class RedisLoginAttemptAdapter implements LoginAttemptPort {
  private static final Logger LOG = LoggerFactory.getLogger(RedisLoginAttemptAdapter.class);
  private static final DefaultRedisScript<Long> FAILURE =
      new DefaultRedisScript<>(
          """
      local count = redis.call('INCR', KEYS[1])
      if count == 1 then redis.call('EXPIRE', KEYS[1], 900) end
      return count
      """,
          Long.class);
  private final ObjectProvider<StringRedisTemplate> redisProvider;

  RedisLoginAttemptAdapter(ObjectProvider<StringRedisTemplate> redisProvider) {
    this.redisProvider = redisProvider;
  }

  @Override
  public boolean isBlocked(String email, String sourceIp) {
    try {
      StringRedisTemplate redis = redisProvider.getIfAvailable();
      if (redis == null) {
        unavailable();
        return false;
      }
      String count = redis.opsForValue().get(key(email, sourceIp));
      return count != null && Long.parseLong(count) >= 5;
    } catch (DataAccessException | NumberFormatException ex) {
      unavailable();
      return false;
    }
  }

  @Override
  public void recordFailure(String email, String sourceIp) {
    try {
      StringRedisTemplate redis = redisProvider.getIfAvailable();
      if (redis == null) {
        unavailable();
        return;
      }
      redis.execute(FAILURE, List.of(key(email, sourceIp)));
    } catch (DataAccessException ex) {
      unavailable();
    }
  }

  @Override
  public void clear(String email, String sourceIp) {
    try {
      StringRedisTemplate redis = redisProvider.getIfAvailable();
      if (redis == null) {
        unavailable();
        return;
      }
      redis.delete(key(email, sourceIp));
    } catch (DataAccessException ex) {
      unavailable();
    }
  }

  private static String key(String email, String sourceIp) {
    return "identity:ratelimit:login:" + email + ":" + sourceIp;
  }

  private static void unavailable() {
    LOG.warn("Login rate limiting unavailable; allowing authentication without throttling (BR-01)");
  }
}
