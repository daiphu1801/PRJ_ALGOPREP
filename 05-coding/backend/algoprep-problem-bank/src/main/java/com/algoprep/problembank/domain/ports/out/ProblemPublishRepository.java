package com.algoprep.problembank.domain.ports.out;

import com.algoprep.problembank.domain.model.ProblemStatusWrite;
import java.util.UUID;

/** Status writes of DD F2 section 40; every call joins the caller's transaction. */
public interface ProblemPublishRepository {

  /**
   * Sets the status of a problem the caller has locked and stamps {@code updated_by} and {@code
   * updated_at}. Publishing also sets {@code published_at}; withdrawing keeps the old value.
   */
  ProblemStatusWrite setStatus(UUID problemId, UUID actorId, String status);

  /**
   * Persists a complete envelope in {@code problem.outbox_events}; never publishes before commit.
   */
  void appendEvent(UUID problemId, String eventType, String routingKey, String envelope);
}
