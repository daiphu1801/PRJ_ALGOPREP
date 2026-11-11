package com.algoprep.aireview.application.ports.in;

import com.algoprep.aireview.application.dto.SweepOutcome;

/** JOB-AI-04: scores or abandons running sessions nobody touched for the idle window. */
public interface SweepIdleInterviewSessionsUseCase {
  SweepOutcome sweep();
}
