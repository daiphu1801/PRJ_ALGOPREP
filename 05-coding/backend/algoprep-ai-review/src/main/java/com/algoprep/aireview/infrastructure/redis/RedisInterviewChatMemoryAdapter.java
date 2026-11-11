package com.algoprep.aireview.infrastructure.redis;

import com.algoprep.aireview.application.ports.out.InterviewChatMemoryPort;
import com.algoprep.aireview.domain.model.ChatMessage;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

/**
 * BR-AI-10: the memory is one JSON string under {@code ai:chatmemory:<sessionId>} with a TTL that
 * restarts on every write. ponytail: whole-value read-modify-write is safe because every writer
 * holds the per-session lock; switch to a Redis list only if turns get very long. Redis errors are
 * logged at WARN and never reach the caller (the database rebuild is always possible).
 */
@Component
class RedisInterviewChatMemoryAdapter implements InterviewChatMemoryPort {
  private static final Logger LOG =
      Logger.getLogger(RedisInterviewChatMemoryAdapter.class.getName());
  private static final JsonMapper JSON = JsonMapper.builder().build();
  private static final String PREFIX = "ai:chatmemory:";

  private final ObjectProvider<StringRedisTemplate> redis;
  private final Duration ttl;

  RedisInterviewChatMemoryAdapter(
      ObjectProvider<StringRedisTemplate> redis,
      @Value("${ai.chatmemory.ttl-hours:2}") int ttlHours) {
    this.redis = redis;
    this.ttl = Duration.ofHours(ttlHours);
  }

  @Override
  public Optional<List<ChatMessage>> read(UUID sessionId) {
    try {
      String raw = redis.getObject().opsForValue().get(PREFIX + sessionId);
      if (raw == null) {
        return Optional.empty();
      }
      return Optional.of(JSON.readValue(raw, new TypeReference<List<ChatMessage>>() {}));
    } catch (RuntimeException failure) {
      LOG.log(Level.WARNING, "ChatMemory read failed for " + sessionId + "; rebuilding", failure);
      return Optional.empty();
    }
  }

  @Override
  public void write(UUID sessionId, List<ChatMessage> messages) {
    try {
      redis
          .getObject()
          .opsForValue()
          .set(PREFIX + sessionId, JSON.writeValueAsString(messages), ttl);
    } catch (RuntimeException failure) {
      LOG.log(Level.WARNING, "ChatMemory write failed for " + sessionId, failure);
    }
  }
}
