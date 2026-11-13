package com.algoprep.judge.application.ports.in;

import com.algoprep.judge.application.dto.AdminQueueFlag;
import java.util.UUID;

/** API #17: stores the absolute autoscale flag, without waiting for the worker poll. */
public interface SetAdminQueueAutoscaleUseCase {
  AdminQueueFlag setAutoscale(UUID actor, boolean expectedEnabled, boolean enabled);
}
