package com.algoprep.aireview.application.ports.in.usage;

import com.algoprep.aireview.application.dto.usage.AnomalyRunOutcome;

/** JOB-AI-05. A role lookup failure propagates so the run is skipped and retried next hour. */
public interface DetectAiUsageAnomaliesUseCase {
  AnomalyRunOutcome detect();
}
