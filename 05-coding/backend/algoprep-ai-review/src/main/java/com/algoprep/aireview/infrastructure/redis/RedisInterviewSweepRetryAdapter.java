package com.algoprep.aireview.infrastructure.redis;

import com.algoprep.aireview.application.ports.out.InterviewSweepRetryPort;
import java.time.Duration;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Counter {@code ai:sweep-retry:<sessionId>} with a one-day TTL (JOB-AI-04). Redis errors are
 * logged and answered with 1, so a Redis outage can never abandon a session; it only delays the
 * cap.
 */
@Component
class RedisInterviewSweepRetryAdapter implements InterviewSweepRetryPort {
  private static final Logger LOG =
      Logger.getLogger(RedisInterviewSweepRetryAdapter.class.getName());
  private static final String PREFIX = "ai:sweep-retry:";
  private static final Duration TTL = Duration.ofDays(1);

  private final ObjectProvider<StringRedisTemplate> redis;

  RedisInterviewSweepRetryAdapter(ObjectProvider<StringRedisTemplate> redis) {
    this.redis = redis;
  }

  @Override
  public int recordFailure(UUID sessionId) {
    try {
      StringRedisTemplate template = redis.getObject();
      Long count = template.opsForValue().increment(PREFIX + sessionId);
      template.expire(PREFIX + sessionId, TTL);
      return count == null ? 1 : count.intValue();
    } catch (RuntimeException failure) {
      LOG.log(Level.WARNING, "Sweep retry counter unavailable for " + sessionId, failure);
      return 1;
    }
  }

  @Override
  public void clear(UUID sessionId) {
    try {
      redis.getObject().delete(PREFIX + sessionId);
    } catch (RuntimeException failure) {
      LOG.log(Level.WARNING, "Sweep retry counter not cleared for " + sessionId, failure);
    }
  }
}
