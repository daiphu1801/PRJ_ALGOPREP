package com.algoprep.aireview.infrastructure.redis;

import com.algoprep.aireview.domain.exception.AiProviderUnavailableException;
import com.algoprep.aireview.domain.exception.AiProviderUnavailableException.Reason;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

/**
 * {@code SET key token NX PX ttl} with a random owner token, released by a Lua compare-and-delete
 * so a late release never removes a lock another owner has since taken. No DB connection is held
 * while {@code work} runs (the LLM call).
 *
 * <p>Fail mode: when Redis cannot be reached while acquiring, the call fails closed with AIR-501
 * (the AI path is already degraded, F1 to F4 do not use this lock). When the release fails the lock
 * expires by itself after {@link #TTL}.
 *
 * <p>ponytail: the TTL is a fixed ceiling above the longest LLM wait (turn 60 s, scoring and
 * generation up to 120 s), with no renewal. If a call outlives it the lock is simply gone and a
 * second caller may enter; add a watchdog renewal only if calls ever exceed 120 s.
 */
final class RedisOwnerLock {
  static final Duration TTL = Duration.ofSeconds(150);
  private static final Logger LOG = Logger.getLogger(RedisOwnerLock.class.getName());
  private static final DefaultRedisScript<Long> RELEASE =
      new DefaultRedisScript<>(
          "if redis.call('GET',KEYS[1])==ARGV[1] then return redis.call('DEL',KEYS[1]) end return 0",
          Long.class);

  private final ObjectProvider<StringRedisTemplate> redis;
  private final String prefix;

  RedisOwnerLock(ObjectProvider<StringRedisTemplate> redis, String prefix) {
    this.redis = redis;
    this.prefix = prefix;
  }

  <T> Optional<T> withLock(UUID id, Supplier<T> work) {
    String key = prefix + id;
    String token = UUID.randomUUID().toString();
    Boolean won;
    try {
      won = redis.getObject().opsForValue().setIfAbsent(key, token, TTL);
    } catch (RuntimeException unavailable) {
      throw new AiProviderUnavailableException(Reason.UPSTREAM_PORT, unavailable);
    }
    if (!Boolean.TRUE.equals(won)) {
      return Optional.empty();
    }
    try {
      return Optional.ofNullable(work.get());
    } finally {
      release(key, token);
    }
  }

  private void release(String key, String token) {
    try {
      redis.getObject().execute(RELEASE, List.of(key), token);
    } catch (RuntimeException unavailable) {
      LOG.log(Level.WARNING, "Lock release unavailable; it expires by itself: " + key, unavailable);
    }
  }
}
