package com.algoprep.problembank.infrastructure.cache;

import com.algoprep.problembank.application.ports.out.TestcaseGenerationLockPort;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

/**
 * SET NX EX 600. When Redis is down the run goes ahead (WARN): the conditional quota update still
 * stops a double count, which is the only thing the lock protects beyond cost.
 */
@Component
class RedisTestcaseGenerationLockAdapter implements TestcaseGenerationLockPort {
  static final Duration TTL = Duration.ofMinutes(10);
  private static final String BYPASS = "bypass";
  private static final Logger LOG =
      LoggerFactory.getLogger(RedisTestcaseGenerationLockAdapter.class);
  private static final DefaultRedisScript<Long> RELEASE =
      new DefaultRedisScript<>(
          "if redis.call('GET',KEYS[1])==ARGV[1] then return redis.call('DEL',KEYS[1]) end return 0",
          Long.class);

  private final ObjectProvider<StringRedisTemplate> redis;

  RedisTestcaseGenerationLockAdapter(ObjectProvider<StringRedisTemplate> redis) {
    this.redis = redis;
  }

  @Override
  public String acquire(UUID problemId) {
    String token = UUID.randomUUID().toString();
    try {
      Boolean won = redis.getObject().opsForValue().setIfAbsent(key(problemId), token, TTL);
      return Boolean.TRUE.equals(won) ? token : null;
    } catch (RuntimeException unavailable) {
      LOG.warn("Generation lock unavailable; the run goes ahead");
      return BYPASS;
    }
  }

  @Override
  public void release(UUID problemId, String token) {
    if (token == null || BYPASS.equals(token)) {
      return;
    }
    try {
      redis.getObject().execute(RELEASE, List.of(key(problemId)), token);
    } catch (RuntimeException unavailable) {
      LOG.warn("Generation lock release unavailable; it expires by itself");
    }
  }

  private static String key(UUID problemId) {
    return "problem:gen:lock:" + problemId;
  }
}
