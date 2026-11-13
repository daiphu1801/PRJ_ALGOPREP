package com.algoprep.identity.infrastructure.otp;

import com.algoprep.identity.application.ports.out.PasswordResetOtpPort;
import com.algoprep.identity.domain.model.PasswordResetChallenge;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

/** BR-04 uses Lua to share the five-failure budget across verify/reset on all instances. */
@Component
class RedisPasswordResetOtpAdapter implements PasswordResetOtpPort {
  // A fresh code never gives a fresh guess budget: wrong guesses also count against the account for
  // an
  // hour (10 in the scripts below), so asking for a new code every minute cannot turn five tries
  // per code into thousands a day.
  private static final DefaultRedisScript<Long> ISSUE =
      new DefaultRedisScript<>(
          """
      if redis.call('EXISTS', KEYS[2]) == 1 then return 0 end
      redis.call('HSET', KEYS[1], 'digest', ARGV[1], 'issued_at', ARGV[2], 'failures', 0)
      redis.call('EXPIRE', KEYS[1], 600)
      redis.call('SET', KEYS[2], ARGV[2], 'EX', 60)
      return 1
      """,
          Long.class);
  private static final DefaultRedisScript<String> VERIFY =
      new DefaultRedisScript<>(
          """
      local digest = redis.call('HGET', KEYS[1], 'digest')
      if not digest then return '' end
      if tonumber(redis.call('GET', KEYS[2]) or '0') >= 10 then return '' end
      local failures = tonumber(redis.call('HGET', KEYS[1], 'failures') or '0')
      if failures >= 5 then redis.call('DEL', KEYS[1]); return '' end
      if digest == ARGV[1] then return redis.call('HGET', KEYS[1], 'issued_at') end
      if redis.call('INCR', KEYS[2]) == 1 then redis.call('EXPIRE', KEYS[2], 3600) end
      if redis.call('HINCRBY', KEYS[1], 'failures', 1) >= 5 then redis.call('DEL', KEYS[1]) end
      return ''
      """,
          String.class);
  private static final DefaultRedisScript<Long> CLEAR =
      new DefaultRedisScript<>(
          """
      if redis.call('HGET', KEYS[1], 'issued_at') == ARGV[1] then redis.call('DEL', KEYS[1]) end
      if redis.call('GET', KEYS[2]) == ARGV[1] then redis.call('DEL', KEYS[2]) end
      return 1
      """,
          Long.class);
  private final ObjectProvider<StringRedisTemplate> redis;

  RedisPasswordResetOtpAdapter(ObjectProvider<StringRedisTemplate> redis) {
    this.redis = redis;
  }

  @Override
  public boolean issue(UUID userId, PasswordResetChallenge challenge) {
    return Long.valueOf(1)
        .equals(
            redis
                .getObject()
                .execute(ISSUE, keys(userId), challenge.digest(), challenge.issuedAt().toString()));
  }

  @Override
  public Optional<Instant> verify(UUID userId, String digest) {
    String issued =
        redis
            .getObject()
            .execute(VERIFY, List.of(keys(userId).getFirst(), budgetKey(userId)), digest);
    return issued == null || issued.isEmpty()
        ? Optional.empty()
        : Optional.of(Instant.parse(issued));
  }

  @Override
  public boolean reserveGuidance(UUID userId, Instant issuedAt) {
    return Boolean.TRUE.equals(
        redis
            .getObject()
            .opsForValue()
            .setIfAbsent(keys(userId).get(1), issuedAt.toString(), Duration.ofSeconds(60)));
  }

  @Override
  public void clear(UUID userId, Instant issuedAt) {
    redis.getObject().execute(CLEAR, keys(userId), issuedAt.toString());
  }

  private static String budgetKey(UUID userId) {
    return "identity:pwreset:budget:" + userId;
  }

  private static List<String> keys(UUID userId) {
    return List.of("identity:pwreset:" + userId, "identity:pwreset:resend:" + userId);
  }
}
