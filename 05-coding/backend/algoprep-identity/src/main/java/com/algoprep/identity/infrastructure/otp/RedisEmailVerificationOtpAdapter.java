package com.algoprep.identity.infrastructure.otp;

import com.algoprep.identity.application.ports.out.EmailVerificationOtpPort;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

/** Each Lua script preserves the original TTL and performs the whole operation atomically. */
@Component
class RedisEmailVerificationOtpAdapter implements EmailVerificationOtpPort {
  // A fresh code never gives a fresh guess budget: wrong guesses also count against the account for
  // an
  // hour (10 in the scripts below), so asking for a new code every minute cannot turn five tries
  // per code into thousands a day.
  private static final DefaultRedisScript<Long> ISSUE =
      new DefaultRedisScript<>(
          """
      if redis.call('EXISTS', KEYS[2]) == 1 then return 0 end
      redis.call('HSET', KEYS[1], 'digest', ARGV[1], 'failures', 0)
      redis.call('EXPIRE', KEYS[1], 600)
      redis.call('SET', KEYS[2], ARGV[1], 'EX', 60)
      return 1
      """,
          Long.class);
  private static final DefaultRedisScript<Long> MATCH =
      new DefaultRedisScript<>(
          """
      local digest = redis.call('HGET', KEYS[1], 'digest')
      if not digest then return 0 end
      if tonumber(redis.call('GET', KEYS[2]) or '0') >= 10 then return 0 end
      local failures = tonumber(redis.call('HGET', KEYS[1], 'failures') or '0')
      if failures >= 5 then return 0 end
      if digest == ARGV[1] then return 1 end
      if redis.call('INCR', KEYS[2]) == 1 then redis.call('EXPIRE', KEYS[2], 3600) end
      redis.call('HINCRBY', KEYS[1], 'failures', 1)
      return 0
      """,
          Long.class);
  private static final DefaultRedisScript<Long> CLEAR =
      new DefaultRedisScript<>(
          """
      if redis.call('HGET', KEYS[1], 'digest') == ARGV[1] then
        redis.call('DEL', KEYS[1])
      end
      if redis.call('GET', KEYS[2]) == ARGV[1] then redis.call('DEL', KEYS[2]) end
      return 1
      """,
          Long.class);

  private final ObjectProvider<StringRedisTemplate> redis;

  RedisEmailVerificationOtpAdapter(ObjectProvider<StringRedisTemplate> redis) {
    this.redis = redis;
  }

  @Override
  public boolean issue(UUID userId, String digest) {
    return Long.valueOf(1).equals(redis.getObject().execute(ISSUE, keys(userId), digest));
  }

  @Override
  public boolean matches(UUID userId, String digest) {
    return Long.valueOf(1)
        .equals(
            redis
                .getObject()
                .execute(
                    MATCH,
                    List.of(keys(userId).getFirst(), "identity:emailverify:budget:" + userId),
                    digest));
  }

  @Override
  public void clear(UUID userId, String digest) {
    redis.getObject().execute(CLEAR, keys(userId), digest);
  }

  private static List<String> keys(UUID userId) {
    return List.of("identity:emailverify:" + userId, "identity:emailverify:resend:" + userId);
  }
}
