package com.algoprep.identity.infrastructure.ratelimit;

import com.algoprep.identity.application.ports.out.JoinAttemptLimitPort;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

/**
 * Sorted set {@code identity:class-join:failed:<userId>}, one mark per wrong attempt scored by
 * Redis time. One Lua step prunes marks older than 15 minutes, optionally adds a mark, and returns
 * the count; the key's TTL is renewed on every failure. Redis errors fail open and never count.
 */
@Component
class RedisJoinAttemptLimitAdapter implements JoinAttemptLimitPort {
  private static final Logger LOG = LoggerFactory.getLogger(RedisJoinAttemptLimitAdapter.class);
  private static final int LIMIT = 5;
  private static final DefaultRedisScript<Long> WINDOW =
      new DefaultRedisScript<>(
          """
      local t = redis.call('TIME')
      local now = t[1] * 1000 + math.floor(t[2] / 1000)
      redis.call('ZREMRANGEBYSCORE', KEYS[1], 0, now - 900000)
      if ARGV[1] == '1' then
        redis.call('ZADD', KEYS[1], now, now .. ':' .. ARGV[2])
        redis.call('PEXPIRE', KEYS[1], 900000)
      end
      return redis.call('ZCARD', KEYS[1])
      """,
          Long.class);
  private final ObjectProvider<StringRedisTemplate> redisProvider;

  RedisJoinAttemptLimitAdapter(ObjectProvider<StringRedisTemplate> redisProvider) {
    this.redisProvider = redisProvider;
  }

  @Override
  public boolean isBlocked(UUID userId) {
    Long count = run(userId, false);
    return count != null && count >= LIMIT;
  }

  @Override
  public void recordFailure(UUID userId) {
    run(userId, true);
  }

  @Override
  public void clear(UUID userId) {
    try {
      StringRedisTemplate redis = redisProvider.getIfAvailable();
      if (redis != null) {
        redis.delete(key(userId));
      }
    } catch (DataAccessException ex) {
      LOG.warn("Class-join rate limiting unavailable; counter not cleared");
    }
  }

  private Long run(UUID userId, boolean addMark) {
    try {
      StringRedisTemplate redis = redisProvider.getIfAvailable();
      if (redis == null) {
        LOG.warn("Class-join rate limiting unavailable; allowing the attempt without throttling");
        return null;
      }
      return redis.execute(
          WINDOW, List.of(key(userId)), addMark ? "1" : "0", UUID.randomUUID().toString());
    } catch (DataAccessException ex) {
      LOG.warn("Class-join rate limiting unavailable; allowing the attempt without throttling");
      return null;
    }
  }

  private static String key(UUID userId) {
    return "identity:class-join:failed:" + userId;
  }
}
