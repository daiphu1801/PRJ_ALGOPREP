package com.algoprep.identity.infrastructure.otp;

import com.algoprep.identity.application.dto.EmailChangeIssueResult;
import com.algoprep.identity.application.ports.out.EmailChangeOtpPort;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

/** Each Lua script runs atomically; the code TTL is never extended by a wrong attempt. */
@Component
class RedisEmailChangeOtpAdapter implements EmailChangeOtpPort {
  // KEYS: code hash, 60s cooldown, 24h request counter. ARGV: digest, new e-mail.
  private static final DefaultRedisScript<Long> ISSUE =
      new DefaultRedisScript<>(
          """
      if redis.call('EXISTS', KEYS[2]) == 1 then return 1 end
      local used = tonumber(redis.call('GET', KEYS[3]) or '0')
      if used >= 3 then return 2 end
      if redis.call('INCR', KEYS[3]) == 1 then redis.call('EXPIRE', KEYS[3], 86400) end
      redis.call('HSET', KEYS[1], 'digest', ARGV[1], 'email', ARGV[2], 'failures', 0)
      redis.call('EXPIRE', KEYS[1], 600)
      redis.call('SET', KEYS[2], ARGV[1], 'EX', 60)
      return 0
      """,
          Long.class);
  private static final DefaultRedisScript<String> MATCH =
      new DefaultRedisScript<>(
          """
      local digest = redis.call('HGET', KEYS[1], 'digest')
      if not digest then return false end
      local failures = tonumber(redis.call('HGET', KEYS[1], 'failures') or '0')
      if failures >= 5 then return false end
      if digest == ARGV[1] then return redis.call('HGET', KEYS[1], 'email') end
      redis.call('HINCRBY', KEYS[1], 'failures', 1)
      return false
      """,
          String.class);
  private static final DefaultRedisScript<Long> CLEAR =
      new DefaultRedisScript<>(
          """
      if redis.call('HGET', KEYS[1], 'digest') == ARGV[1] then redis.call('DEL', KEYS[1]) end
      if redis.call('GET', KEYS[2]) == ARGV[1] then redis.call('DEL', KEYS[2]) end
      return 1
      """,
          Long.class);

  private final ObjectProvider<StringRedisTemplate> redis;

  RedisEmailChangeOtpAdapter(ObjectProvider<StringRedisTemplate> redis) {
    this.redis = redis;
  }

  @Override
  public EmailChangeIssueResult issue(UUID userId, String newEmail, String digest) {
    Long code =
        redis
            .getObject()
            .execute(
                ISSUE,
                List.of(codeKey(userId), cooldownKey(userId), budgetKey(userId)),
                digest,
                newEmail);
    return switch (code == null ? -1 : code.intValue()) {
      case 0 -> EmailChangeIssueResult.ISSUED;
      case 1 -> EmailChangeIssueResult.COOLDOWN;
      case 2 -> EmailChangeIssueResult.BUDGET_EXCEEDED;
      default -> throw new IllegalStateException("Unexpected e-mail change issue result");
    };
  }

  @Override
  public Optional<String> match(UUID userId, String digest) {
    return Optional.ofNullable(redis.getObject().execute(MATCH, List.of(codeKey(userId)), digest));
  }

  @Override
  public void clear(UUID userId, String digest) {
    redis.getObject().execute(CLEAR, List.of(codeKey(userId), cooldownKey(userId)), digest);
  }

  private static String codeKey(UUID userId) {
    return "identity:emailchange:" + userId;
  }

  private static String cooldownKey(UUID userId) {
    return "identity:emailchange:resend:" + userId;
  }

  private static String budgetKey(UUID userId) {
    return "identity:emailchange:budget:" + userId;
  }
}
