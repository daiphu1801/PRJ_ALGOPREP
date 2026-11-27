package com.algoprep.aireview.application.ports.out;

import java.util.UUID;

/**
 * In-memory ledger of sessions whose AI reply is being generated right now; feeds {@code replyState
 * = GENERATING} (DD api F5 section 2.4). One backend instance is assumed, as the DD states.
 */
public interface InterviewReplyTracker {
  void begin(UUID sessionId);

  void end(UUID sessionId);

  boolean isGenerating(UUID sessionId);
}
