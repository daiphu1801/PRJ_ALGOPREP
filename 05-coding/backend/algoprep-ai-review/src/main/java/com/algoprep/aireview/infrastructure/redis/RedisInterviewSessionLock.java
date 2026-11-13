package com.algoprep.aireview.infrastructure.redis;

import com.algoprep.aireview.domain.ports.out.InterviewSessionLockPort;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/** Per-session lock {@code ai:session:<sessionId>} for one turn, hint or scoring (BR-AI-07). */
@Component
class RedisInterviewSessionLock implements InterviewSessionLockPort {
  private final RedisOwnerLock lock;

  RedisInterviewSessionLock(ObjectProvider<StringRedisTemplate> redis) {
    this.lock = new RedisOwnerLock(redis, "ai:session:");
  }

  @Override
  public <T> Optional<T> withLock(UUID sessionId, Supplier<T> work) {
    return lock.withLock(sessionId, work);
  }
}
