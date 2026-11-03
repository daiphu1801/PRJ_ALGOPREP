package com.algoprep.judge.domain.ports.out;

import java.util.List;
import java.util.UUID;

public interface JudgeMaintenanceRepository {
  List<UUID> staleCandidates(int limit, int timeoutSeconds);

  /** Transaction-scoped job/worker locks, current pause flag, heartbeat and terminal guard. */
  boolean claimStale(UUID id, int timeoutSeconds);

  boolean tryJobLock(String job);

  int deletePublished(int days, int batchSize);

  int deleteTestcaseDetails(int days, int batchSize);

  boolean lockQueuePaused();

  void changeQueuePaused(boolean paused);

  void refreshPendingHeartbeat();
}
