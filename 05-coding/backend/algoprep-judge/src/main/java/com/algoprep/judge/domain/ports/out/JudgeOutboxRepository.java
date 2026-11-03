package com.algoprep.judge.domain.ports.out;

import com.algoprep.judge.domain.model.OutboxEvent;
import java.util.List;
import java.util.UUID;

public interface JudgeOutboxRepository {
  /** Locks pending rows until the caller's transaction ends; skips other relay owners. */
  List<OutboxEvent> lockPending();

  /** Counts every attempt and marks delivery only after publisher confirmation. */
  void attempted(UUID id, boolean confirmed);
}
