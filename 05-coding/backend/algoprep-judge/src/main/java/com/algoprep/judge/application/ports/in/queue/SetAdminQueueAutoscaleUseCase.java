package com.algoprep.judge.application.ports.in.queue;

import com.algoprep.judge.application.dto.queue.AdminQueueFlag;
import java.util.UUID;

/** API #17: stores the absolute autoscale flag, without waiting for the worker poll. */
public interface SetAdminQueueAutoscaleUseCase {
  AdminQueueFlag setAutoscale(UUID actor, boolean expectedEnabled, boolean enabled);
}
