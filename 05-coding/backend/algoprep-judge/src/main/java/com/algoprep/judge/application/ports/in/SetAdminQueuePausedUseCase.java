package com.algoprep.judge.application.ports.in;

import com.algoprep.judge.application.dto.AdminQueueFlag;
import java.util.UUID;

/** API #16: optimistic pause/resume, heartbeat refresh and REQUIRED audit. */
public interface SetAdminQueuePausedUseCase {
  AdminQueueFlag setPaused(UUID actor, boolean expectedPaused, boolean paused);
}
