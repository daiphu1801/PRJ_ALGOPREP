package com.algoprep.identity.infrastructure.ratelimit;

import com.algoprep.identity.application.ports.out.LoginAttemptPort;
import com.algoprep.identity.domain.exception.RateLimitUnavailableException;
import java.util.List;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

/**
 * BR-01: counter expires 15 minutes after the first failure; unavailable protection fails closed.
 */
@Component
class RedisLoginAttemptAdapter implements LoginAttemptPort {
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
        throw unavailable();
      }
      String count = redis.opsForValue().get(key(email, sourceIp));
      if (count == null) {
        return false;
      }
      long failures = Long.parseLong(count);
      if (failures < 0) {
        throw unavailable();
      }
      return failures >= 5;
    } catch (DataAccessException | NumberFormatException ex) {
      throw unavailable();
    }
  }

  @Override
  public void recordFailure(String email, String sourceIp) {
    try {
      StringRedisTemplate redis = redisProvider.getIfAvailable();
      if (redis == null) {
        throw unavailable();
      }
      Long count = redis.execute(FAILURE, List.of(key(email, sourceIp)));
      if (count == null || count < 1) {
        throw unavailable();
      }
    } catch (DataAccessException ex) {
      throw unavailable();
    }
  }

  @Override
  public void clear(String email, String sourceIp) {
    try {
      StringRedisTemplate redis = redisProvider.getIfAvailable();
      if (redis == null) {
        throw unavailable();
      }
      if (redis.delete(key(email, sourceIp)) == null) {
        throw unavailable();
      }
    } catch (DataAccessException ex) {
      throw unavailable();
    }
  }

  private static String key(String email, String sourceIp) {
    return "identity:ratelimit:login:" + email + ":" + sourceIp;
  }

  private static RateLimitUnavailableException unavailable() {
    return new RateLimitUnavailableException();
  }
}
