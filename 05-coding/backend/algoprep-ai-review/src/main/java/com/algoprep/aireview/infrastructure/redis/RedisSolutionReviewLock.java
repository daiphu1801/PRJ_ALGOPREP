package com.algoprep.aireview.infrastructure.redis;

import com.algoprep.aireview.domain.ports.out.SolutionReviewLockPort;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/** Per-submission lock {@code ai:review:<submissionId>} across the LLM call (BR-AI-02 step 4). */
@Component
class RedisSolutionReviewLock implements SolutionReviewLockPort {
  private final RedisOwnerLock lock;

  RedisSolutionReviewLock(ObjectProvider<StringRedisTemplate> redis) {
    this.lock = new RedisOwnerLock(redis, "ai:review:");
  }

  @Override
  public <T> Optional<T> withLock(UUID submissionId, Supplier<T> work) {
    return lock.withLock(submissionId, work);
  }
}
