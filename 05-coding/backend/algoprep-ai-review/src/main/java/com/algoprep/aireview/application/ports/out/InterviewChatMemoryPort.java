package com.algoprep.aireview.application.ports.out;

import com.algoprep.aireview.domain.model.ChatMessage;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Short-term interview memory in Redis, key {@code ai:chatmemory:<sessionId>} (BR-AI-10). Not a
 * source of truth: {@code interview_turns} is. A Redis failure never throws; it reads as "missing"
 * and writes become a no-op, so the caller falls back to rebuilding from the database.
 */
public interface InterviewChatMemoryPort {
  /** Empty when the key is missing, expired or Redis is down. */
  Optional<List<ChatMessage>> read(UUID sessionId);

  /** Replaces the whole memory and restarts the 2 hour TTL. */
  void write(UUID sessionId, List<ChatMessage> messages);
}
