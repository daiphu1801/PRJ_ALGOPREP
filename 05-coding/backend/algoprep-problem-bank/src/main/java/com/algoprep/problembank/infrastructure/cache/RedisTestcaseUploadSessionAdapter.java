package com.algoprep.problembank.infrastructure.cache;

import com.algoprep.problembank.application.dto.TestcaseUploadSession.Session;
import com.algoprep.problembank.application.ports.out.TestcaseUploadSessionPort;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/**
 * {@code problem:upload:<uploadId>} as JSON with a TTL. Unlike the list-version cache this cannot
 * degrade silently: without the session a confirm has nothing to check, so failures propagate.
 */
@Component
class RedisTestcaseUploadSessionAdapter implements TestcaseUploadSessionPort {
  private static final JsonMapper JSON = JsonMapper.builder().build();
  private final ObjectProvider<StringRedisTemplate> redis;

  RedisTestcaseUploadSessionAdapter(ObjectProvider<StringRedisTemplate> redis) {
    this.redis = redis;
  }

  @Override
  public void save(Session session, Duration ttl) {
    redis.getObject().opsForValue().set(key(session.id()), JSON.writeValueAsString(session), ttl);
  }

  @Override
  public Optional<Session> find(UUID uploadId) {
    String json = redis.getObject().opsForValue().get(key(uploadId));
    return json == null ? Optional.empty() : Optional.of(JSON.readValue(json, Session.class));
  }

  @Override
  public void update(Session session) {
    var client = redis.getObject();
    Long left = client.getExpire(key(session.id()), TimeUnit.MILLISECONDS);
    if (left == null || left <= 0) {
      return;
    }
    client
        .opsForValue()
        .setIfPresent(key(session.id()), JSON.writeValueAsString(session), Duration.ofMillis(left));
  }

  private static String key(UUID uploadId) {
    return "problem:upload:" + uploadId;
  }
}
