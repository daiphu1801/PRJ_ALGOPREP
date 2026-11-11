package com.algoprep.aireview.application.ports.in;

import com.algoprep.aireview.application.dto.AnomalyRunOutcome;

/** JOB-AI-05. A role lookup failure propagates so the run is skipped and retried next hour. */
public interface DetectAiUsageAnomaliesUseCase {
  AnomalyRunOutcome detect();
}
