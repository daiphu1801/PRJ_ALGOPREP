package com.algoprep.aireview.application.ports.out;

import com.algoprep.aireview.application.dto.TurnAccepted;
import com.algoprep.aireview.application.dto.TurnCompleted;
import java.util.concurrent.CompletableFuture;

/** Where a running turn reports its events; implementations must never throw. */
public interface InterviewTurnSink {
  void accepted(TurnAccepted event);

  void token(String text);

  void completed(TurnCompleted event);

  /** The turn failed after {@code accepted}; the USER line stays committed. */
  void failed(RuntimeException failure);

  /**
   * Completes when the client is gone (DEC-2026-1110-f5-turn-budget-gate-and-cancel-on-disconnect):
   * the turn then cancels the LLM stream and commits nothing. The default never completes.
   */
  default CompletableFuture<Void> disconnected() {
    return new CompletableFuture<>();
  }
}
