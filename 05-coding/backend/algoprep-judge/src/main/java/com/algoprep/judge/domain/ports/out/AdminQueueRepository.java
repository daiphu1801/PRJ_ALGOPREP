package com.algoprep.judge.domain.ports.out;

import com.algoprep.judge.domain.model.AdminQueueState;
import java.util.Map;
import java.util.UUID;

/** Own-schema singleton; writes run under the caller's row lock and audit transaction. */
public interface AdminQueueRepository {
  AdminQueueState read();

  AdminQueueState lock();

  /** The existing pause kernel already wrote the flag, timestamp and resume heartbeats. */
  void markPauseActor(UUID actor);

  void setAutoscale(boolean enabled, UUID actor);

  /** Only the four DD setting names are accepted; no arbitrary SQL identifiers. */
  void saveSettings(Map<String, Integer> changed, UUID actor);
}
