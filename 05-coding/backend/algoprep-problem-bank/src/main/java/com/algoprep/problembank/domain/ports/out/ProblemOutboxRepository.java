package com.algoprep.problembank.domain.ports.out;

import com.algoprep.problembank.domain.model.OutboxEvent;
import java.util.List;
import java.util.UUID;

public interface ProblemOutboxRepository {
  /** Locks pending rows until the caller's transaction ends; skips other relay owners. */
  List<OutboxEvent> lockPending(int limit);

  /** Counts every attempt and marks delivery only after publisher confirmation. */
  void attempted(UUID id, boolean confirmed);

  /** Deletes published rows older than the retention; rows never published are kept. */
  int deletePublishedOlderThanDays(int days, int limit);
}
