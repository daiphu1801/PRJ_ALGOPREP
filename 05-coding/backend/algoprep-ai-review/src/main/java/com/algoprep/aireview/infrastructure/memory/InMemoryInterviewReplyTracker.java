package com.algoprep.aireview.infrastructure.memory;

import com.algoprep.aireview.application.ports.out.InterviewReplyTracker;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/** ponytail: per-instance set; move to Redis only if the backend ever runs more than one node. */
@Component
class InMemoryInterviewReplyTracker implements InterviewReplyTracker {
  private final Set<UUID> running = ConcurrentHashMap.newKeySet();

  @Override
  public void begin(UUID sessionId) {
    running.add(sessionId);
  }

  @Override
  public void end(UUID sessionId) {
    running.remove(sessionId);
  }

  @Override
  public boolean isGenerating(UUID sessionId) {
    return running.contains(sessionId);
  }
}
