package com.algoprep.aireview.domain.ports.out;

import com.algoprep.aireview.domain.model.AiOutboxEvent;
import java.util.List;
import java.util.UUID;

/**
 * Table {@code ai.outbox_events}: the writer joins the caller's transaction, the relay owns its
 * own.
 */
public interface AiOutboxRepository {
  /** Inserts one pending row in the caller's transaction (it must roll back with the aggregate). */
  void insert(AiOutboxEvent event, String aggregateType, UUID aggregateId);

  /** Locks pending rows until the caller's transaction ends; skips rows locked by another relay. */
  List<AiOutboxEvent> lockPending(int limit);

  /** Counts every attempt and marks delivery only after publisher confirmation. */
  void attempted(UUID id, boolean confirmed);

  /** Transaction-scoped advisory try-lock so only one instance cleans per run. */
  boolean tryCleanupLock();

  /** Deletes published rows older than the retention; rows never published are kept. */
  int deletePublishedOlderThanDays(int days, int limit);
}
